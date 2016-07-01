package services

import scala.concurrent.Future

import cats.implicits._
import failures.{NotFoundFailure400, NotFoundFailure404, OpenTransactionsFailure}
import models.customer.{Customer, Customers}
import models.payment.storecredit.StoreCredit.Canceled
import models.payment.storecredit.StoreCreditSubtypes.scope._
import models.payment.storecredit._
import models.{Reason, Reasons, StoreAdmin}
import payloads.PaymentPayloads._
import payloads.StoreCreditPayloads._
import responses.StoreCreditBulkResponse._
import responses.StoreCreditResponse._
import responses.{StoreCreditResponse, StoreCreditSubTypesResponse}
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object StoreCreditService {
  type QuerySeq = StoreCredits.QuerySeq

  def getOriginTypes(implicit ec: EC, db: DB): DbResultT[Seq[StoreCreditSubTypesResponse.Root]] =
    StoreCreditSubtypes.result.map { subTypes ⇒
      StoreCreditSubTypesResponse.build(StoreCredit.OriginType.publicTypes.toSeq, subTypes)
    }.toXor

  // Check subtype only if id is present in payload; discard actual model
  private def checkSubTypeExists(subTypeId: Option[Int], originType: StoreCredit.OriginType)(
      implicit ec: EC): DbResultT[Unit] = {
    subTypeId.fold(DbResultT.unit) { subtypeId ⇒
      StoreCreditSubtypes
        .byOriginType(originType)
        .filter(_.id === subtypeId)
        .one
        .toXor
        .flatMap(_.fold {
          DbResultT.failure[Unit](NotFoundFailure400(StoreCreditSubtype, subtypeId))
        } { _ ⇒
          DbResultT.unit
        })
    }
  }

  def totalsForCustomer(customerId: Int)(implicit ec: EC,
                                         db: DB): Result[StoreCreditResponse.Totals] =
    (for {
      _      ← * <~ Customers.mustFindById404(customerId)
      totals ← * <~ fetchTotalsForCustomer(customerId)
    } yield totals).map(_.getOrElse(Totals(0, 0))).value.run()

  def fetchTotalsForCustomer(customerId: Int)(implicit ec: EC): DBIO[Option[Totals]] = {
    StoreCredits
      .findAllActiveByCustomerId(customerId)
      .groupBy(_.customerId)
      .map { case (_, q) ⇒ (q.map(_.availableBalance).sum, q.map(_.currentBalance).sum) }
      .one
      .map(_.map {
        case (avail, curr) ⇒ StoreCreditResponse.Totals(avail.getOrElse(0), curr.getOrElse(0))
      })
  }

  def createManual(admin: StoreAdmin, customerId: Int, payload: CreateManualStoreCredit)(
      implicit ec: EC,
      db: DB,
      ac: AC): Result[Root] = {
    val reason400 = NotFoundFailure400(Reason, payload.reasonId)
    (for {
      customer ← * <~ Customers.mustFindById404(customerId)
      _        ← * <~ Reasons.findById(payload.reasonId).extract.mustFindOneOr(reason400)
      _        ← * <~ checkSubTypeExists(payload.subTypeId, StoreCredit.CsrAppeasement)
      manual = StoreCreditManual(adminId = admin.id,
                                 reasonId = payload.reasonId,
                                 subReasonId = payload.subReasonId)
      origin ← * <~ StoreCreditManuals.create(manual)
      appeasement = StoreCredit
        .buildAppeasement(customerId = customer.id, originId = origin.id, payload = payload)
      storeCredit ← * <~ StoreCredits.create(appeasement)
      _           ← * <~ LogActivity.scCreated(admin, customer, storeCredit)
    } yield build(storeCredit)).runTxn
  }

  // API routes

  def createFromExtension(admin: StoreAdmin, customerId: Int, payload: CreateExtensionStoreCredit)(
      implicit ec: EC,
      db: DB,
      ac: AC): Result[Root] =
    (for {
      customer ← * <~ Customers.mustFindById404(customerId)
      _        ← * <~ checkSubTypeExists(payload.subTypeId, StoreCredit.Custom)
      custom = StoreCreditCustom(adminId = admin.id, metadata = payload.metadata)
      origin ← * <~ StoreCreditCustoms.create(custom)
      customSC = StoreCredit.buildFromExtension(customerId = customer.id,
                                                payload = payload,
                                                originType = StoreCredit.Custom,
                                                originId = origin.id)
      storeCredit ← * <~ StoreCredits.create(customSC)
      _           ← * <~ LogActivity.scCreated(admin, customer, storeCredit)
    } yield build(storeCredit)).runTxn

  def getById(id: Int)(implicit ec: EC, db: DB): Result[Root] =
    (for {
      storeCredit ← * <~ StoreCredits.mustFindById404(id)
    } yield StoreCreditResponse.build(storeCredit)).run()

  def getByIdAndCustomer(storeCreditId: Int, customer: Customer)(implicit ec: EC,
                                                                 db: DB): Result[Root] =
    (for {
      storeCredit ← * <~ StoreCredits
                     .findByIdAndCustomerId(storeCreditId, customer.id)
                     .mustFindOr(NotFoundFailure404(StoreCredit, storeCreditId))
    } yield StoreCreditResponse.build(storeCredit)).run()

  def bulkUpdateStateByCsr(payload: StoreCreditBulkUpdateStateByCsr, admin: StoreAdmin)(
      implicit ec: EC,
      db: DB,
      ac: AC): Result[Seq[ItemResult]] =
    (for {
      _ ← ResultT.fromXor(payload.validate.toXor)
      response ← ResultT.right(Future.sequence(payload.ids.map { id ⇒
                  val itemPayload = StoreCreditUpdateStateByCsr(payload.state, payload.reasonId)
                  updateStateByCsr(id, itemPayload, admin).map(buildItemResult(id, _))
                }))
    } yield response).value

  def updateStateByCsr(id: Int, payload: StoreCreditUpdateStateByCsr, admin: StoreAdmin)(
      implicit ec: EC,
      db: DB,
      ac: AC): Result[Root] =
    (for {
      _           ← * <~ payload.validate
      storeCredit ← * <~ StoreCredits.mustFindById404(id)
      updated     ← * <~ cancelOrUpdate(storeCredit, payload.state, payload.reasonId, admin)
      _           ← * <~ LogActivity.scUpdated(admin, storeCredit, payload)
    } yield StoreCreditResponse.build(updated)).runTxn()

  private def cancelOrUpdate(storeCredit: StoreCredit,
                             newState: StoreCredit.State,
                             reasonId: Option[Int],
                             admin: StoreAdmin)(implicit ec: EC, db: DB) = newState match {
    case Canceled ⇒
      for {
        _ ← * <~ StoreCreditAdjustments
             .lastAuthByStoreCreditId(storeCredit.id)
             .one
             .mustNotFindOr(OpenTransactionsFailure)
        _ ← * <~ reasonId.map(id ⇒ Reasons.mustFindById400(id)).getOrElse(DbResultT.unit)
        upd ← * <~ StoreCredits.update(storeCredit,
                                       storeCredit.copy(state = newState,
                                                        canceledReason = reasonId,
                                                        canceledAmount =
                                                          storeCredit.availableBalance.some))
        _ ← * <~ StoreCredits.cancelByCsr(storeCredit, admin)
      } yield upd

    case _ ⇒ StoreCredits.update(storeCredit, storeCredit.copy(state = newState))
  }
}
