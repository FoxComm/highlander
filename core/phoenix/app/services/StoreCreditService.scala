package services

import cats.implicits._
import failures.{NotFoundFailure400, NotFoundFailure404, OpenTransactionsFailure}
import models.account.{User, Users}
import models.payment.storecredit.StoreCredit.Canceled
import models.payment.storecredit.StoreCreditSubtypes.scope._
import models.payment.storecredit._
import models.{Reason, Reasons}
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
    }.dbresult

  // Check subtype only if id is present in payload; discard actual model
  private def checkSubTypeExists(subTypeId: Option[Int], originType: StoreCredit.OriginType)(
      implicit ec: EC): DbResultT[Unit] = {
    subTypeId.fold(DbResultT.unit) { subtypeId ⇒
      StoreCreditSubtypes
        .byOriginType(originType)
        .filter(_.id === subtypeId)
        .one
        .dbresult
        .flatMap(_.fold {
          DbResultT.failure[Unit](NotFoundFailure400(StoreCreditSubtype, subtypeId))
        } { _ ⇒
          DbResultT.unit
        })
    }
  }

  def totalsForCustomer(accountId: Int)(implicit ec: EC,
                                        db: DB): DbResultT[StoreCreditResponse.Totals] =
    for {
      _      ← * <~ Users.mustFindByAccountId(accountId)
      totals ← * <~ fetchTotalsForCustomer(accountId)
    } yield totals.getOrElse(Totals(0, 0))

  def fetchTotalsForCustomer(accountId: Int)(implicit ec: EC): DBIO[Option[Totals]] = {
    StoreCredits
      .findAllActiveByAccountId(accountId)
      .groupBy(_.accountId)
      .map { case (_, q) ⇒ (q.map(_.availableBalance).sum, q.map(_.currentBalance).sum) }
      .one
      .map(_.map {
        case (avail, curr) ⇒ StoreCreditResponse.Totals(avail.getOrElse(0), curr.getOrElse(0))
      })
  }

  def createManual(admin: User, accountId: Int, payload: CreateManualStoreCredit)(
      implicit ec: EC,
      db: DB,
      ac: AC): DbResultT[Root] = {
    val reason400 = NotFoundFailure400(Reason, payload.reasonId)
    for {
      customer ← * <~ Users.mustFindByAccountId(accountId)
      _        ← * <~ Reasons.findById(payload.reasonId).extract.mustFindOneOr(reason400)
      _        ← * <~ checkSubTypeExists(payload.subTypeId, StoreCredit.CsrAppeasement)
      manual = StoreCreditManual(adminId = admin.accountId,
                                 reasonId = payload.reasonId,
                                 subReasonId = payload.subReasonId)
      origin ← * <~ StoreCreditManuals.create(manual)
      appeasement = StoreCredit
        .buildAppeasement(accountId = customer.accountId, originId = origin.id, payload = payload)
      storeCredit ← * <~ StoreCredits.create(appeasement)
      _           ← * <~ LogActivity.scCreated(admin, customer, storeCredit)
    } yield build(storeCredit)
  }

  // API routes

  def createFromExtension(admin: User, accountId: Int, payload: CreateExtensionStoreCredit)(
      implicit ec: EC,
      db: DB,
      ac: AC): DbResultT[Root] =
    for {
      customer ← * <~ Users.mustFindByAccountId(accountId)
      _        ← * <~ checkSubTypeExists(payload.subTypeId, StoreCredit.Custom)
      custom = StoreCreditCustom(adminId = admin.accountId, metadata = payload.metadata)
      origin ← * <~ StoreCreditCustoms.create(custom)
      customSC = StoreCredit.buildFromExtension(accountId = customer.accountId,
                                                payload = payload,
                                                originType = StoreCredit.Custom,
                                                originId = origin.id)
      storeCredit ← * <~ StoreCredits.create(customSC)
      _           ← * <~ LogActivity.scCreated(admin, customer, storeCredit)
    } yield build(storeCredit)

  def getById(id: Int)(implicit ec: EC, db: DB): DbResultT[Root] =
    for {
      storeCredit ← * <~ StoreCredits.mustFindById404(id)
    } yield StoreCreditResponse.build(storeCredit)

  def getByIdAndCustomer(storeCreditId: Int, customer: User)(implicit ec: EC,
                                                             db: DB): DbResultT[Root] =
    for {
      storeCredit ← * <~ StoreCredits
                     .findByIdAndAccountId(storeCreditId, customer.accountId)
                     .mustFindOr(NotFoundFailure404(StoreCredit, storeCreditId))
    } yield StoreCreditResponse.build(storeCredit)

  def bulkUpdateStateByCsr(
      payload: StoreCreditBulkUpdateStateByCsr,
      admin: User)(implicit ec: EC, db: DB, ac: AC): DbResultT[Seq[ItemResult]] =
    for {
      _ ← * <~ payload.validate.toXor
      response ← * <~ payload.ids.map { id ⇒
                  val itemPayload = StoreCreditUpdateStateByCsr(payload.state, payload.reasonId)
                  updateStateByCsr(id, itemPayload, admin).value
                    .map(buildItemResult(id, _))
                    .dbresult
                }
    } yield response

  def updateStateByCsr(id: Int,
                       payload: StoreCreditUpdateStateByCsr,
                       admin: User)(implicit ec: EC, db: DB, ac: AC): DbResultT[Root] =
    for {
      _           ← * <~ payload.validate
      storeCredit ← * <~ StoreCredits.mustFindById404(id)
      updated     ← * <~ cancelOrUpdate(storeCredit, payload.state, payload.reasonId, admin)
      _           ← * <~ LogActivity.scUpdated(admin, storeCredit, payload)
    } yield StoreCreditResponse.build(updated)

  private def cancelOrUpdate(storeCredit: StoreCredit,
                             newState: StoreCredit.State,
                             reasonId: Option[Int],
                             admin: User)(implicit ec: EC, db: DB) = newState match {
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
