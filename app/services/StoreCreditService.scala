package services

import scala.concurrent.{ExecutionContext, Future}

import cats.implicits._
import cats.data.Xor
import models.StoreCredit.Canceled
import models.StoreCreditSubtypes.scope._
import models.{Customers, Reason, Reasons, StoreAdmin, StoreCredit, StoreCreditAdjustments, StoreCreditManual,
StoreCreditManuals, StoreCreditSubtype, StoreCreditSubtypes, StoreCredits}
import payloads.StoreCreditUpdateStatusByCsr
import responses.StoreCreditBulkResponse._
import responses.StoreCreditResponse._
import responses.{StoreCreditResponse, StoreCreditSubTypesResponse}
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives.SortAndPage
import utils.DbResultT
import utils.Slick._
import utils.Slick.implicits._
import utils.DbResultT.implicits._
import utils.DbResultT._

import models.activity.ActivityContext

object StoreCreditService {
  type QuerySeq = StoreCredits.QuerySeq

  def getOriginTypes(implicit db: Database, ec: ExecutionContext): Result[Seq[StoreCreditSubTypesResponse.Root]] =
    Result.fromFuture(StoreCreditSubtypes.result.map { subTypes ⇒
      StoreCreditSubTypesResponse.build(StoreCredit.OriginType.types.toSeq, subTypes)
    }.run())

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

  def getById(id: Int)(implicit db: Database, ec: ExecutionContext): Result[Root] = (for {
    storeCredit ← * <~ StoreCredits.mustFindById(id)
  } yield StoreCreditResponse.build(storeCredit)).run()

  def bulkUpdateStatusByCsr(payload: payloads.StoreCreditBulkUpdateStatusByCsr, admin: StoreAdmin)
    (implicit ec: ExecutionContext, db: Database, ac: ActivityContext): Result[Seq[ItemResult]] = (for {
    _        ← ResultT.fromXor(payload.validate.toXor)
    response ← ResultT.right(Future.sequence(payload.ids.map { id ⇒
                 val itemPayload = StoreCreditUpdateStatusByCsr(payload.status, payload.reasonId)
                 updateStatusByCsr(id, itemPayload, admin).map(buildItemResult(id, _))
               }))
  } yield response).value

  def updateStatusByCsr(id: Int, payload: payloads.StoreCreditUpdateStatusByCsr, admin: StoreAdmin)
    (implicit ec: ExecutionContext, db: Database, ac: ActivityContext): Result[Root] = (for {
    _           ← * <~ payload.validate
    storeCredit ← * <~ StoreCredits.mustFindById(id)
    updated     ← * <~ cancelOrUpdate(storeCredit, payload.status, payload.reasonId, admin)
    _           ← * <~ LogActivity.scUpdated(admin, storeCredit, payload)
  } yield StoreCreditResponse.build(updated)).runTxn()

  private def cancelOrUpdate(storeCredit: StoreCredit, newState: StoreCredit.Status, reasonId: Option[Int],
    admin: StoreAdmin)(implicit ec: ExecutionContext, db: Database) = newState match {
    case Canceled ⇒ for {
      _   ← * <~ StoreCreditAdjustments.lastAuthByStoreCreditId(storeCredit.id).one.mustNotFindOr(OpenTransactionsFailure)
      _   ← * <~ reasonId.map(id ⇒ Reasons.mustFindById(id, _ ⇒ InvalidCancellationReasonFailure)).getOrElse(DbResult.unit)
      upd ← * <~ StoreCredits.update(storeCredit, storeCredit.copy(status = newState, canceledReason = reasonId,
                   canceledAmount = storeCredit.availableBalance.some))
      _   ← * <~ StoreCredits.cancelByCsr(storeCredit, admin)
    } yield upd

    case _ ⇒ DbResultT(StoreCredits.update(storeCredit, storeCredit.copy(status = newState)))
  }
}
