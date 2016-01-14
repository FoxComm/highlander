package services

import scala.concurrent.{ExecutionContext, Future}

import cats.data.Validated.{Invalid, Valid}
import cats.data.Xor
import models.StoreCredit.Canceled
import models.StoreCreditSubtypes.scope._
import models.{Customers, Reason, Reasons, StoreAdmin, StoreCredit, StoreCreditAdjustments, StoreCreditManual,
StoreCreditManuals, StoreCreditSubtype, StoreCreditSubtypes, StoreCredits}
import responses.StoreCreditBulkResponse._
import responses.StoreCreditResponse._
import responses.{StoreCreditResponse, StoreCreditSubTypesResponse}
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives.SortAndPage
import utils.Slick.UpdateReturning._
import utils.Slick._
import utils.Slick.implicits._
import utils.DbResultT.implicits._
import utils.DbResultT._

import models.activity.ActivityContext

object StoreCreditService {
  type QuerySeq = StoreCredits.QuerySeq

  def getOriginTypes(implicit db: Database, ec: ExecutionContext): Result[Seq[StoreCreditSubTypesResponse.Root]] = {
    StoreCreditSubtypes.select { subTypes ⇒
      DbResult.good(StoreCreditSubTypesResponse.build(StoreCredit.OriginType.types.toSeq, subTypes))
    }
  }

  def findAllByCustomer(customerId: Int)
    (implicit db: Database, ec: ExecutionContext, sp: SortAndPage): Result[ResponseWithMetadata[WithTotals]] = (for {

    _           ← * <~ Customers.mustFindById(customerId)
    query       = StoreCredits.findAllByCustomerId(customerId)
    paginated   = StoreCredits.sortedAndPaged(query)

    sc          ← * <~ query.result.map(StoreCreditResponse.build).toXor
    totals      ← * <~ fetchTotalsForCustomer(customerId).toXor
    withTotals  = WithTotals(storeCredits = sc, totals = totals)
    response    ← * <~ ResultWithMetadata(result = DbResult.good(withTotals), metadata = paginated.metadata)

  } yield response).value.run().flatMap {
    case Xor.Left(f)    ⇒ Result.failures(f)
    case Xor.Right(res) ⇒ res.asResponseFuture.flatMap(Result.good)
  }

  def totalsForCustomer(customerId: Int)
    (implicit db: Database, ec: ExecutionContext): Result[StoreCreditResponse.Totals] = (for {
    _       ← * <~ Customers.mustFindById(customerId)
    totals  ← * <~ fetchTotalsForCustomer(customerId).toXor
  } yield totals).map(_.getOrElse(Totals(0, 0))).value.run()

  def fetchTotalsForCustomer(customerId: Int)
    (implicit db: Database, ec: ExecutionContext): DBIO[Option[Totals]] = {
    StoreCredits.findAllActiveByCustomerId(customerId)
      .groupBy(_.customerId)
      .map { case (_, q) ⇒ (q.map(_.availableBalance).sum, q.map(_.currentBalance).sum) }
      .one
      .map(_.map { case (avail, curr) ⇒ StoreCreditResponse.Totals(avail.getOrElse(0), curr.getOrElse(0)) })
  }

  def createManual(admin: StoreAdmin, customerId: Int, payload: payloads.CreateManualStoreCredit)
    (implicit db: Database, ec: ExecutionContext, ac: ActivityContext): Result[Root] = (for {
    customer ← * <~ Customers.mustFindById(customerId)
    _ ← * <~ Reasons.findById(payload.reasonId).extract.one.mustFindOr(NotFoundFailure400(Reason, payload.reasonId))
    // Check subtype only if id is present in payload; discard actual model
    _ ← * <~ payload.subTypeId.fold(DbResult.unit) { subtypeId ⇒
      StoreCreditSubtypes.csrAppeasements.filter(_.id === subtypeId).one.flatMap(_.fold {
        DbResult.failure[Unit](NotFoundFailure400(StoreCreditSubtype, subtypeId))
      } { _ ⇒ DbResult.unit })
    }
    manual = StoreCreditManual(adminId = admin.id, reasonId = payload.reasonId, subReasonId = payload.subReasonId)
    origin ← * <~ StoreCreditManuals.create(manual)
    appeasement = StoreCredit.buildAppeasement(customerId = customer.id, originId = origin.id, payload = payload)
    storeCredit ← * <~ StoreCredits.create(appeasement)
    _ ← * <~ LogActivity.scCreated(admin, customer, storeCredit)
  } yield build(storeCredit)).runTxn

  def getById(id: Int)(implicit db: Database, ec: ExecutionContext): Result[Root] = {
    fetchDetails(id).run().flatMap {
      case Some(storeCredit) ⇒
        Result.right(responses.StoreCreditResponse.build(storeCredit))
      case _ ⇒
        Result.failure(NotFoundFailure404(StoreCredit, id))
    }
  }

  def bulkUpdateStatusByCsr(payload: payloads.StoreCreditBulkUpdateStatusByCsr, admin: StoreAdmin)
    (implicit ec: ExecutionContext, db: Database, ac: ActivityContext): Result[Seq[ItemResult]] = {

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
    (implicit ec: ExecutionContext, db: Database, ac: ActivityContext): Result[Root] = {

    def cancelOrUpdate(finder: QuerySeq, sc: StoreCredit): DbResult[Root] = (payload.status, payload.reasonId) match {
      case (Canceled, Some(reason)) ⇒
        cancelByCsr(finder, sc, payload, admin)
      case (Canceled, None) ⇒
        DbResult.failure(EmptyCancellationReasonFailure)
      case (_, _) ⇒
        val ifNotFound = NotFoundFailure404(StoreCredit, sc.id)

        LogActivity.scUpdated(admin, sc, payload) >>
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
    admin: StoreAdmin)(implicit ec: ExecutionContext, db: Database, ac: ActivityContext): DbResult[Root] = {

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

            cancellation.flatMap { xor ⇒
              xorMapDbio(xor){ root ⇒
                LogActivity.scUpdated(admin, sc, payload) >> cancelAdjustment >> lift(root)
              }
            }
        }
    }
  }

  private def fetchDetails(id: Int)(implicit db: Database, ec: ExecutionContext) = for {
    storeCredit ← StoreCredits.findOneById(id)
  } yield storeCredit
}
