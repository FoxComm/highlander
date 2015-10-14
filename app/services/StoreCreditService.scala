package services

import scala.concurrent.{ExecutionContext, Future}

import cats.data.Validated.{Valid, Invalid}
import cats.data.Xor
import cats.implicits._

import models.StoreCredit.Canceled
import models._
import models.StoreCreditSubtypes.scope._
import responses.StoreCreditResponse
import responses.StoreCreditResponse._
import responses.StoreCreditBulkResponse._
import slick.driver.PostgresDriver.api._
import utils.Slick._
import utils.Slick.UpdateReturning._
import utils.Slick.implicits._

object StoreCreditService {
  type QuerySeq = Query[StoreCredits, StoreCredit, Seq]

  type StoreCreditDependencies = ResultT[(Customer, Reason, Option[StoreCreditSubtype])]

  def createManual(admin: StoreAdmin, customerId: Int, payload: payloads.CreateManualStoreCredit)
    (implicit db: Database, ec: ExecutionContext): Result[Root] = {

    def prepareForCreate(customerId: Int, payload: payloads.CreateManualStoreCredit)
      (implicit db: Database, ec: ExecutionContext): StoreCreditDependencies = {
      val queries = for {
        customer ← Customers.findOneById(customerId)
        reason   ← Reasons.findOneById(payload.reasonId)
        subtype  ← payload.subTypeId match {
          case Some(id) ⇒ StoreCreditSubtypes.csrAppeasements.filter(_.id === id).one
          case None     ⇒ lift(None)
        }
      } yield (customer, reason, subtype)

      ResultT(queries.run().map {
        case (Some(c), Some(r), s) ⇒ Xor.right((c, r, s))
        case (None, _, _)          ⇒ Xor.left(NotFoundFailure(Customer, customerId).single)
        case (_, None, _)          ⇒ Xor.left(NotFoundFailure(Reason, payload.reasonId).single)
      })
    }

    def saveStoreCredit(admin: StoreAdmin, customer: Customer, payload: payloads.CreateManualStoreCredit)
      (implicit db: Database, ec: ExecutionContext): ResultT[DBIO[Root]] = {

      val actions = for {
        origin ← StoreCreditManuals.save(StoreCreditManual(adminId = admin.id, reasonId = payload.reasonId,
          subReasonId = payload.subReasonId))
        sc ← StoreCredits.save(StoreCredit.buildAppeasement(customerId = customer.id, originId = origin.id,
          payload = payload))
      } yield sc

      ResultT.rightAsync(actions.flatMap(sc ⇒ lift(build(sc))))
    }

    val transformer = for {
      prepare ← prepareForCreate(customerId, payload)
      sc ← prepare match {
        case (customer, reason, subtype) ⇒ saveStoreCredit(admin, customer, payload.copy(subTypeId = subtype.map(_.id)))
      }
    } yield sc

    transformer.value.flatMap(_.fold(Result.left, dbio ⇒ Result.fromFuture(dbio.transactionally.run())))
  }

  def getById(id: Int)(implicit db: Database, ec: ExecutionContext): Result[Root] = {
    fetchDetails(id).run().flatMap {
      case Some(storeCredit) ⇒
        Result.right(responses.StoreCreditResponse.build(storeCredit))
      case _ ⇒
        Result.failure(NotFoundFailure(StoreCredit, id))
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

    def cancelOrUpdate(finder: QuerySeq, sc: StoreCredit) = (payload.status, payload.reasonId) match {
      case (Canceled, Some(reason)) ⇒
        cancelByCsr(finder, sc, payload, admin)
      case (Canceled, None) ⇒
        DbResult.failure(EmptyCancellationReasonFailure)
      case (_, _) ⇒
        val update = finder.map(_.status).updateReturning(StoreCredits.map(identity), payload.status).headOption
        update.flatMap {
          case Some(gc) ⇒ DbResult.good(StoreCreditResponse.build(gc))
          case _        ⇒ DbResult.failure(NotFoundFailure(StoreCredit, sc.id))
        }
    }

    payload.validate match {
      case Valid(_) ⇒
        val finder = StoreCredits.filter(_.id === id)

        finder.selectOneForUpdate { sc ⇒
          sc.transitionTo(payload.status) match {
            case Xor.Left(message) ⇒ DbResult.failure(GeneralFailure(message))
            case Xor.Right(_)      ⇒ cancelOrUpdate(finder, sc)
          }
        }
      case Invalid(errors) ⇒
        Result.failures(errors)
    }
  }

  private def cancelByCsr(finder: QuerySeq, sc: StoreCredit, payload: payloads.StoreCreditUpdateStatusByCsr,
    admin: StoreAdmin)(implicit ec: ExecutionContext, db: Database) = {

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
              .updateReturning(StoreCredits.map(identity), data)
              .head

            val cancelAdjustment = StoreCredits.cancelByCsr(sc, admin)

            DbResult.fromDbio(cancelAdjustment >> cancellation.flatMap {
              sc ⇒ lift(StoreCreditResponse.build(sc))
            })
        }
    }
  }

  private def fetchDetails(id: Int)(implicit db: Database, ec: ExecutionContext) = for {
    storeCredit ← StoreCredits.findOneById(id)
  } yield storeCredit
}

