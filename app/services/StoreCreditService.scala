package services

import scala.concurrent.{ExecutionContext, Future}

import cats.data.Validated.{Valid, Invalid}
import cats.data.Xor
import cats.implicits._

import models.StoreCredit.Canceled
import models.StoreCredit.{CsrAppeasement, GiftCardTransfer, ReturnProcess}
import models._
import models.StoreCreditSubtypes.scope._
import responses.StoreCreditResponse
import responses.StoreCreditResponse._
import responses.StoreCreditBulkResponse._
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives.SortAndPage
import utils.Slick._
import utils.Slick.UpdateReturning._
import utils.Slick.implicits._

object StoreCreditService {
  type QuerySeq = StoreCredits.QuerySeq

  def getOriginTypes: Seq[StoreCredit.OriginType] = StoreCredit.OriginType.types.toSeq

  def getSubTypes(originType: String)(implicit db: Database, ec: ExecutionContext): Result[Seq[StoreCreditSubtype]] = {
    StoreCredit.OriginType.read(originType) match {
      case Some(CsrAppeasement)   ⇒ StoreCreditSubtypes.csrAppeasements.select(DbResult.good)
      case Some(GiftCardTransfer) ⇒ StoreCreditSubtypes.giftCardTransfers.select(DbResult.good)
      case Some(ReturnProcess)    ⇒ StoreCreditSubtypes.returnProcesses.select(DbResult.good)
      case _                      ⇒ Result.failure(InvalidOriginTypeFailure)
    }
  }

  def sortedAndPaged(query: QuerySeq)
    (implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): QuerySeq = {

    val sortedQuery = sortAndPage.sort match {
      case Some(s) ⇒ query.sortBy { storeCredit ⇒
        s.sortColumn match {
          case "id"               ⇒ if(s.asc) storeCredit.id.asc               else storeCredit.id.desc
          case "originId"         ⇒ if(s.asc) storeCredit.originId.asc         else storeCredit.originId.desc
          case "originType"       ⇒ if(s.asc) storeCredit.originType.asc       else storeCredit.originType.desc
          case "customerId"       ⇒ if(s.asc) storeCredit.customerId.asc       else storeCredit.customerId.desc
          case "currency"         ⇒ if(s.asc) storeCredit.currency.asc         else storeCredit.currency.desc
          case "originalBalance"  ⇒ if(s.asc) storeCredit.originalBalance.asc  else storeCredit.originalBalance.desc
          case "currentBalance"   ⇒ if(s.asc) storeCredit.currentBalance.asc   else storeCredit.currentBalance.desc
          case "availableBalance" ⇒ if(s.asc) storeCredit.availableBalance.asc else storeCredit.availableBalance.desc
          case "canceledAmount"   ⇒ if(s.asc) storeCredit.canceledAmount.asc   else storeCredit.canceledAmount.desc
          case "canceledReason"   ⇒ if(s.asc) storeCredit.canceledReason.asc   else storeCredit.canceledReason.desc
          case "createdAt"        ⇒ if(s.asc) storeCredit.createdAt.asc      else storeCredit.createdAt.desc
          case _                  ⇒ storeCredit.id.asc
        }
      }
      case None    ⇒ query
    }

    sortedQuery.paged
  }

  def findAllByCustomer(customerId: Int)
    (implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): Result[Seq[Root]] = {

    val query = StoreCredits.findAllByCustomerId(customerId)

    Result.fromFuture(sortedAndPaged(query).result.run().map(StoreCreditResponse.build))
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
      } yield (customer, reason, subtype)

      ResultT(queries.run().map {
        case (None, _, _) ⇒
          Xor.left(NotFoundFailure404(Customer, customerId).single)
        case (_, None, _) ⇒
          Xor.left(NotFoundFailure400(Reason, payload.reasonId).single)
        case (_, _, None) if payload.subTypeId.isDefined ⇒
          Xor.left(NotFoundFailure400(StoreCreditSubtype, payload.subTypeId.head).single)
        case (Some(c), Some(r), s) ⇒
          Xor.right((c, r, s))
      })
    }

    def saveStoreCredit(admin: StoreAdmin, customer: Customer, payload: payloads.CreateManualStoreCredit):
      ResultT[DBIO[Root]] = {

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

    def cancelOrUpdate(finder: QuerySeq, sc: StoreCredit) = (payload.status, payload.reasonId) match {
      case (Canceled, Some(reason)) ⇒
        cancelByCsr(finder, sc, payload, admin)
      case (Canceled, None) ⇒
        DbResult.failure(EmptyCancellationReasonFailure)
      case (_, _) ⇒
        val update = finder.map(_.status).updateReturning(StoreCredits.map(identity), payload.status).headOption
        update.flatMap {
          case Some(gc) ⇒ DbResult.good(StoreCreditResponse.build(gc))
          case _        ⇒ DbResult.failure(NotFoundFailure404(StoreCredit, sc.id))
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

