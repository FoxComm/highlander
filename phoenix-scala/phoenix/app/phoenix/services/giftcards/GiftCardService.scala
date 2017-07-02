package phoenix.services.giftcards

import cats.implicits._
import core.db._
import core.failures.NotFoundFailure400
import phoenix.failures.OpenTransactionsFailure
import phoenix.models.Reasons
import phoenix.models.account._
import phoenix.models.customer._
import phoenix.models.payment.giftcard.GiftCard.Canceled
import phoenix.models.payment.giftcard.GiftCardSubtypes.scope._
import phoenix.models.payment.giftcard._
import phoenix.payloads.GiftCardPayloads._
import phoenix.responses.giftcards._
import phoenix.responses.users._
import phoenix.services._
import phoenix.utils.aliases._
import slick.jdbc.PostgresProfile.api._

object GiftCardService {
  type QuerySeq = GiftCards.QuerySeq

  def getOriginTypes(implicit ec: EC, db: DB): DbResultT[Seq[GiftCardSubTypesResponse]] =
    for {
      subTypes ← * <~ GiftCardSubtypes.result
      response ← * <~ GiftCardSubTypesResponse.build(GiftCard.OriginType.types.toSeq, subTypes)
    } yield response

  def getByCode(code: String)(implicit ec: EC, db: DB): DbResultT[GiftCardResponse] =
    for {
      giftCard ← * <~ GiftCards.mustFindByCode(code)
      response ← * <~ buildResponse(giftCard)
    } yield response

  private def buildResponse(giftCard: GiftCard)(implicit ec: EC) =
    (giftCard.originType, giftCard.accountId) match {
      case (GiftCard.CsrAppeasement, _) ⇒
        for {
          origin ← * <~ GiftCardManuals
                    .filter(_.id === giftCard.originId)
                    .mustFindOneOr(NotFoundFailure400(GiftCardManuals, giftCard.originId))
          admin ← * <~ Users.mustFindByAccountId(origin.adminId)
        } yield GiftCardResponse.build(giftCard, None, Some(UserResponse.build(admin)))

      case (GiftCard.CustomerPurchase, Some(accountId)) ⇒
        for {
          customer ← * <~ Users.mustFindByAccountId(accountId)
          custData ← * <~ CustomersData.mustFindByAccountId(accountId)
        } yield GiftCardResponse.build(giftCard, Some(CustomerResponse.build(customer, custData)), None)
      case _ ⇒ GiftCardResponse.build(giftCard, None, None).pure[DbResultT]
    }

  def createByAdmin(
      admin: User,
      payload: GiftCardCreateByCsr)(implicit ec: EC, db: DB, ac: AC, au: AU): DbResultT[GiftCardResponse] =
    for {
      scope ← * <~ Scope.resolveOverride(payload.scope)
      _     ← * <~ Reasons.mustFindById400(payload.reasonId)
      // If `subTypeId` is absent, don't query. Check for model existence otherwise.
      subtype ← * <~ payload.subTypeId.traverse { subId ⇒
                 GiftCardSubtypes.csrAppeasements
                   .filter(_.id === subId)
                   .mustFindOneOr(NotFoundFailure400(GiftCardSubtype, subId))
               }
      origin ← * <~ GiftCardManuals.create(
                GiftCardManual(adminId = admin.accountId, reasonId = payload.reasonId))
      giftCard ← * <~ GiftCards.create(GiftCard.buildAppeasement(payload, origin.id, scope))
      adminResp = Some(UserResponse.build(admin))
      _ ← * <~ LogActivity().withScope(scope).gcCreated(admin, giftCard)
    } yield GiftCardResponse.build(gc = giftCard, admin = adminResp)

  def createByCustomer(admin: User, payload: GiftCardCreatedByCustomer)(implicit ec: EC,
                                                                        db: DB,
                                                                        ac: AC,
                                                                        au: AU): DbResultT[GiftCardResponse] =
    for {
      scope  ← * <~ Scope.resolveOverride(payload.scope)
      origin ← * <~ GiftCardOrders.create(GiftCardOrder(cordRef = payload.cordRef))
      adminResp = UserResponse.build(admin).some
      giftCard ← * <~ GiftCards.create(GiftCard.buildByCustomerPurchase(payload, origin.id, scope))
      _        ← * <~ LogActivity().withScope(scope).gcCreated(admin, giftCard)
    } yield GiftCardResponse.build(gc = giftCard, admin = adminResp)

  def createBulkByAdmin(admin: User, payload: GiftCardBulkCreateByCsr)(
      implicit ec: EC,
      db: DB,
      ac: AC,
      au: AU): DbResultT[List[GiftCardBulkResponse]] =
    for {
      scope ← * <~ Scope.resolveOverride(payload.scope)
      gcCreatePayload = GiftCardCreateByCsr(balance = payload.balance,
                                            reasonId = payload.reasonId,
                                            currency = payload.currency,
                                            scope = scope.toString.some)
      response ← * <~ DbResultT.seqCollectFailures((1 to payload.quantity).map { num ⇒
                  createByAdmin(admin, gcCreatePayload).mapEitherRight(GiftCardBulkResponse.build(_))
                }.toList)
    } yield response

  def bulkUpdateStateByCsr(
      payload: GiftCardBulkUpdateStateByCsr,
      admin: User)(implicit ec: EC, db: DB, ac: AC): DbResultT[List[GiftCardBulkResponse]] =
    DbResultT.seqCollectFailures(payload.codes.map { code ⇒
      val itemPayload = GiftCardUpdateStateByCsr(payload.state, payload.reasonId)
      updateStateByCsr(code, itemPayload, admin).mapEitherRight(GiftCardBulkResponse.build(_, Some(code)))
    }.toList)

  def updateStateByCsr(code: String,
                       payload: GiftCardUpdateStateByCsr,
                       admin: User)(implicit ec: EC, db: DB, ac: AC): DbResultT[GiftCardResponse] =
    for {
      _        ← * <~ payload.reasonId.traverse(Reasons.mustFindById400)
      giftCard ← * <~ GiftCards.mustFindByCode(code)
      updated  ← * <~ cancelOrUpdate(giftCard, payload.state, payload.reasonId, admin)
      _        ← * <~ LogActivity().gcUpdated(admin, giftCard, payload)
    } yield GiftCardResponse.build(updated)

  private def cancelOrUpdate(giftCard: GiftCard,
                             newState: GiftCard.State,
                             reasonId: Option[Int],
                             admin: User)(implicit ec: EC) = newState match {
    case Canceled ⇒
      for {
        _ ← * <~ GiftCardAdjustments
             .lastAuthByGiftCardId(giftCard.id)
             .mustNotFindOneOr(OpenTransactionsFailure)
        upd ← * <~ GiftCards.update(giftCard,
                                    giftCard.copy(state = newState,
                                                  canceledReason = reasonId,
                                                  canceledAmount = giftCard.availableBalance.some))
        _ ← * <~ GiftCards.cancelByCsr(giftCard, admin)
      } yield upd

    case other ⇒ GiftCards.update(giftCard, giftCard.copy(state = other))
  }
}
