package services

import cats.data.Validated.{Invalid, Valid}
import cats.data.Xor
import cats.implicits._
import models.StoreCredit.Canceled
import models.StoreCreditSubtypes.scope._
import models.{Customer, Customers, Reason, Reasons, StoreAdmin, StoreCredit, StoreCreditAdjustments, StoreCreditManual,
StoreCreditManuals, StoreCreditSubtype, StoreCreditSubtypes, StoreCredits}
import responses.StoreCreditBulkResponse._
import responses.StoreCreditResponse._
import responses.{StoreCreditResponse, StoreCreditSubTypesResponse}
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives.SortAndPage
import utils.Slick.UpdateReturning._
import utils.Slick._
import utils.Slick.implicits._

import scala.concurrent.{ExecutionContext, Future}

object StoreCreditService {
  type QuerySeq = StoreCredits.QuerySeq

  def getOriginTypes(implicit db: Database, ec: ExecutionContext): Result[Seq[StoreCreditSubTypesResponse.Root]] = {
    StoreCreditSubtypes.select { subTypes ⇒
      DbResult.good(StoreCreditSubTypesResponse.build(StoreCredit.OriginType.types.toSeq, subTypes))
    }
  }

  def findAllByCustomer(customerId: Int)
    (implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): ResultWithMetadata[Seq[Root]] = {
    val query = StoreCredits.queryByCustomer(customerId)
    query.result.map(_.map(StoreCreditResponse.build))
  }

  def createManual(admin: StoreAdmin, customerId: Int, payload: payloads.CreateManualStoreCredit)
    (implicit db: Database, ec: ExecutionContext): Result[Root] = {

    def prepareForCreate(customerId: Int, payload: payloads.CreateManualStoreCredit):
      ResultT[(Customer, Reason, Option[StoreCreditSubtype])] = {

      val queries = for {
        customer ← Customers.findOneById(customerId)
        reason   ← Reasons.findOneById(payload.reasonId)
        subtype  ← payload.subTypeId match {
          case Some(id) ⇒ StoreCreditSubtypes.csrAppeasements.filter(_.id === id).one
          case None     ⇒ lift(None)
        }
      } yield (customer, reason, subtype, payload.subTypeId)

      ResultT(queries.run().map {
        case (None, _, _, _) ⇒
          Xor.left(NotFoundFailure404(Customer, customerId).single)
        case (_, None, _, _) ⇒
          Xor.left(NotFoundFailure400(Reason, payload.reasonId).single)
        case (_, _, None, Some(subId)) ⇒
          Xor.left(NotFoundFailure400(StoreCreditSubtype, subId).single)
        case (Some(c), Some(r), s, _) ⇒
          Xor.right((c, r, s))
      })
    }

    def saveStoreCredit(admin: StoreAdmin, customer: Customer, payload: payloads.CreateManualStoreCredit):
      ResultT[DBIO[Root]] = {

      val actions = for {
        origin ← StoreCreditManuals.saveNew(StoreCreditManual(adminId = admin.id, reasonId = payload.reasonId,
          subReasonId = payload.subReasonId))
        sc ← StoreCredits.saveNew(StoreCredit.buildAppeasement(customerId = customer.id, originId = origin.id,
          payload = payload))
      } yield sc

      ResultT.rightAsync(actions.flatMap(sc ⇒ lift(build(sc))))
    }

    val transformer = for {
      prepare ← prepareForCreate(customerId, payload)
      sc ← prepare match { case (customer, reason, subtype) ⇒
        val newPayload = payload.copy(subTypeId = subtype.map(_.id))
        saveStoreCredit(admin, customer, newPayload)
      }
    } yield sc

    transformer.value.flatMap(_.fold(Result.left, dbio ⇒ Result.fromFuture(dbio.transactionally.run())))
  }

  def getById(id: Int)(implicit db: Database, ec: ExecutionContext): Result[Root] = {
    fetchDetails(id).run().flatMap {
      case Some(storeCredit) ⇒
        Result.right(responses.StoreCreditResponse.build(storeCredit))
      case _ ⇒
        Result.failure(NotFoundFailure404(StoreCredit, id))
    }
  }

  def bulkUpdateStatusByCsr(payload: payloads.StoreCreditBulkUpdateStatusByCsr, admin: StoreAdmin)
    (implicit ec: ExecutionContext, db: Database): Result[Seq[ItemResult]] = {

    payload.validate match {
      case Valid(_) ⇒
        val responses = payload.ids.map { id ⇒
          val itemPayload = payloads.StoreCreditUpdateStatusByCsr(payload.status, payload.reasonId)
          updateStatusByCsr(id, itemPayload, admin).map(buildItemResult(id, _))
        }

        val future = Future.sequence(responses).flatMap { seq ⇒
          Future.successful(seq)
        }

        Result.fromFuture(future)
      case Invalid(errors) ⇒
        Result.failures(errors)
    }
  }

  def updateStatusByCsr(id: Int, payload: payloads.StoreCreditUpdateStatusByCsr, admin: StoreAdmin)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = {

    def cancelOrUpdate(finder: QuerySeq, sc: StoreCredit): DbResult[Root] = (payload.status, payload.reasonId) match {
      case (Canceled, Some(reason)) ⇒
        cancelByCsr(finder, sc, payload, admin)
      case (Canceled, None) ⇒
        DbResult.failure(EmptyCancellationReasonFailure)
      case (_, _) ⇒
        val ifNotFound = NotFoundFailure404(StoreCredit, sc.id)
        finder.map(_.status)
          .updateReturningHeadOption(StoreCredits.map(identity), payload.status, ifNotFound)
          .map(_.map(StoreCreditResponse.build))
    }

    payload.validate match {
      case Valid(_) ⇒
        val finder = StoreCredits.filter(_.id === id)

        finder.selectOneForUpdate { sc ⇒
          sc.transitionState(payload.status) match {
            case Xor.Left(message) ⇒ DbResult.failures(message)
            case Xor.Right(_)      ⇒ cancelOrUpdate(finder, sc)
          }
        }
      case Invalid(errors) ⇒
        Result.failures(errors)
    }
  }

  private def cancelByCsr(finder: QuerySeq, sc: StoreCredit, payload: payloads.StoreCreditUpdateStatusByCsr,
    admin: StoreAdmin)(implicit ec: ExecutionContext, db: Database): DbResult[Root] = {

    StoreCreditAdjustments.lastAuthByStoreCreditId(sc.id).one.flatMap {
      case Some(adjustment) ⇒
        DbResult.failure(OpenTransactionsFailure)
      case None ⇒
        Reasons.findOneById(payload.reasonId.get).flatMap {
          case None ⇒
            DbResult.failure(InvalidCancellationReasonFailure)
          case _ ⇒
            val data = (payload.status, Some(sc.availableBalance), payload.reasonId)
            val cancellation = finder
              .map { gc ⇒ (gc.status, gc.canceledAmount, gc.canceledReason) }
              .updateReturningHead(StoreCredits.map(identity), data)
              .map(_.map(StoreCreditResponse.build))

            val cancelAdjustment = StoreCredits.cancelByCsr(sc, admin)

            cancellation.flatMap { xor ⇒ xorMapDbio(xor)(sc ⇒ cancelAdjustment >> lift(sc)) }
        }
    }
  }

  private def fetchDetails(id: Int)(implicit db: Database, ec: ExecutionContext) = for {
    storeCredit ← StoreCredits.findOneById(id)
  } yield storeCredit
}
