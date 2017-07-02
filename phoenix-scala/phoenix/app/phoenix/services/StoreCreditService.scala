package phoenix.services

import cats.implicits._
import core.db._
import core.failures.{NotFoundFailure400, NotFoundFailure404}
import phoenix.failures.OpenTransactionsFailure
import phoenix.models.account.{Scope, User, Users}
import phoenix.models.payment.storecredit.StoreCredit.Canceled
import phoenix.models.payment.storecredit.StoreCreditSubtypes.scope._
import phoenix.models.payment.storecredit._
import phoenix.models.{Reason, Reasons}
import phoenix.payloads.PaymentPayloads._
import phoenix.payloads.StoreCreditPayloads._
import phoenix.responses.StoreCreditBulkResponse._
import phoenix.responses.StoreCreditResponse._
import phoenix.responses.{StoreCreditResponse, StoreCreditSubTypesResponse, StoreCreditTotalsResponse}
import phoenix.utils.aliases._
import slick.jdbc.PostgresProfile.api._
import core.utils.Money._

object StoreCreditService {
  type QuerySeq = StoreCredits.QuerySeq

  def getOriginTypes(implicit ec: EC, db: DB): DbResultT[Seq[StoreCreditSubTypesResponse]] =
    StoreCreditSubtypes.result.map { subTypes ⇒
      StoreCreditSubTypesResponse.build(StoreCredit.OriginType.publicTypes.toSeq, subTypes)
    }.dbresult

  // Check subtype only if id is present in payload; discard actual model
  private def checkSubTypeExists(subTypeId: Option[Int], originType: StoreCredit.OriginType)(
      implicit ec: EC): DbResultT[Unit] =
    subTypeId.traverse { subtypeId ⇒
      StoreCreditSubtypes
        .byOriginType(originType)
        .filter(_.id === subtypeId)
        .one
        .dbresult
        .flatMap(_.fold {
          DbResultT.failure[Unit](NotFoundFailure400(StoreCreditSubtype, subtypeId))
        } { _ ⇒
          ().pure[DbResultT]
        })
    }.void

  def totalsForCustomer(accountId: Int)(implicit ec: EC, db: DB): DbResultT[StoreCreditTotalsResponse] =
    for {
      _      ← * <~ Users.mustFindByAccountId(accountId)
      totals ← * <~ fetchTotalsForCustomer(accountId)
    } yield totals.getOrElse(StoreCreditTotalsResponse(0, 0))

  def fetchTotalsForCustomer(accountId: Int)(implicit ec: EC): DBIO[Option[StoreCreditTotalsResponse]] =
    StoreCredits
      .findAllActiveByAccountId(accountId)
      .groupBy(_.accountId)
      .map { case (_, q) ⇒ (q.map(_.availableBalance).sum, q.map(_.currentBalance).sum) }
      .one
      .map(_.map {
        case (avail, curr) ⇒
          StoreCreditTotalsResponse(avail.getOrElse(0L), curr.getOrElse(0L))
      })

  def createManual(admin: User, accountId: Int, payload: CreateManualStoreCredit)(
      implicit ec: EC,
      db: DB,
      ac: AC,
      au: AU): DbResultT[StoreCreditResponse] = {
    val reason400 = NotFoundFailure400(Reason, payload.reasonId)
    for {
      customer ← * <~ Users.mustFindByAccountId(accountId)
      _        ← * <~ Reasons.findById(payload.reasonId).extract.mustFindOneOr(reason400)
      _        ← * <~ checkSubTypeExists(payload.subTypeId, StoreCredit.CsrAppeasement)
      scope    ← * <~ Scope.resolveOverride(payload.scope)
      manual = StoreCreditManual(adminId = admin.accountId,
                                 reasonId = payload.reasonId,
                                 subReasonId = payload.subReasonId)
      origin ← * <~ StoreCreditManuals.create(manual)
      storeCredit ← * <~ StoreCredits.create(
                     StoreCredit(
                       accountId = customer.accountId,
                       originId = origin.id,
                       scope = scope,
                       originType = StoreCredit.CsrAppeasement,
                       subTypeId = payload.subTypeId,
                       currency = payload.currency,
                       originalBalance = payload.amount
                     ))
      _ ← * <~ LogActivity().withScope(storeCredit.scope).scCreated(admin, customer, storeCredit)
    } yield build(storeCredit)
  }

  // API routes

  def createFromExtension(admin: User, accountId: Int, payload: CreateExtensionStoreCredit)(
      implicit ec: EC,
      db: DB,
      ac: AC,
      au: AU): DbResultT[StoreCreditResponse] =
    for {
      customer ← * <~ Users.mustFindByAccountId(accountId)
      scope    ← * <~ Scope.resolveOverride(payload.scope)
      _        ← * <~ checkSubTypeExists(payload.subTypeId, StoreCredit.Custom)
      custom = StoreCreditCustom(adminId = admin.accountId, metadata = payload.metadata)
      origin ← * <~ StoreCreditCustoms.create(custom)
      storeCredit ← * <~ StoreCredits.create(
                     StoreCredit(
                       accountId = customer.accountId,
                       originType = StoreCredit.Custom,
                       originId = origin.id,
                       currency = payload.currency,
                       subTypeId = payload.subTypeId,
                       originalBalance = payload.amount,
                       scope = scope
                     ))
      _ ← * <~ LogActivity().withScope(scope).scCreated(admin, customer, storeCredit)
    } yield build(storeCredit)

  def getById(id: Int)(implicit ec: EC, db: DB): DbResultT[StoreCreditResponse] =
    for {
      storeCredit ← * <~ StoreCredits.mustFindById404(id)
    } yield StoreCreditResponse.build(storeCredit)

  def getByIdAndCustomer(storeCreditId: Int, customer: User)(implicit ec: EC,
                                                             db: DB): DbResultT[StoreCreditResponse] =
    for {
      storeCredit ← * <~ StoreCredits
                     .findByIdAndAccountId(storeCreditId, customer.accountId)
                     .mustFindOr(NotFoundFailure404(StoreCredit, storeCreditId))
    } yield StoreCreditResponse.build(storeCredit)

  def bulkUpdateStateByCsr(payload: StoreCreditBulkUpdateStateByCsr,
                           admin: User)(implicit ec: EC, db: DB, ac: AC): DbResultT[List[ItemResult]] =
    for {
      response ← * <~ DbResultT.seqCollectFailures(payload.ids.map { id ⇒
                  val itemPayload = StoreCreditUpdateStateByCsr(payload.state, payload.reasonId)
                  updateStateByCsr(id, itemPayload, admin)
                    .mapEitherRight(buildItemResult(id, _)) // FIXME: for God’s sake, use the standard error/warning reporting @michalrus
                }.toList)
    } yield response

  def updateStateByCsr(id: Int,
                       payload: StoreCreditUpdateStateByCsr,
                       admin: User)(implicit ec: EC, db: DB, ac: AC): DbResultT[StoreCreditResponse] =
    for {
      storeCredit ← * <~ StoreCredits.mustFindById404(id)
      updated     ← * <~ cancelOrUpdate(storeCredit, payload.state, payload.reasonId, admin)
      _           ← * <~ LogActivity().scUpdated(admin, storeCredit, payload)
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
        _ ← * <~ reasonId.traverse(Reasons.mustFindById400)
        upd ← * <~ StoreCredits.update(storeCredit,
                                       storeCredit.copy(state = newState,
                                                        canceledReason = reasonId,
                                                        canceledAmount = storeCredit.availableBalance.some))
        _ ← * <~ StoreCredits.cancelByCsr(storeCredit, admin)
      } yield upd

    case _ ⇒ StoreCredits.update(storeCredit, storeCredit.copy(state = newState))
  }
}
