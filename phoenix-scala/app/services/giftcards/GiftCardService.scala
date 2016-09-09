package services.giftcards

import cats.implicits._
import failures.{NotFoundFailure400, OpenTransactionsFailure}
import models.account.Users
import models.payment.giftcard.GiftCard.Canceled
import models.payment.giftcard.GiftCardSubtypes.scope._
import models.payment.giftcard._
import models.{Reasons, StoreAdmin, StoreAdmins}
import payloads.GiftCardPayloads._
import responses.GiftCardBulkResponse._
import responses.GiftCardResponse._
import responses.{CustomerResponse, GiftCardResponse, GiftCardSubTypesResponse, StoreAdminResponse}
import services._
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object GiftCardService {
  type QuerySeq = GiftCards.QuerySeq

  def getOriginTypes(implicit ec: EC, db: DB): DbResultT[Seq[GiftCardSubTypesResponse.Root]] =
    for {
      subTypes ← * <~ GiftCardSubtypes.result
      response ← * <~ GiftCardSubTypesResponse.build(GiftCard.OriginType.types.toSeq, subTypes)
    } yield response

  def getByCode(code: String)(implicit ec: EC, db: DB): DbResultT[Root] =
    for {
      giftCard ← * <~ GiftCards.mustFindByCode(code)
      response ← * <~ buildResponse(giftCard)
    } yield response

  private def buildResponse(giftCard: GiftCard)(implicit ec: EC) =
    (giftCard.originType, giftCard.accountId) match {
      case (GiftCard.CsrAppeasement, _) ⇒
        for {
          origin ← GiftCardManuals.filter(_.id === giftCard.originId).one
          admin ← origin
                   .map(o ⇒ StoreAdmins.findOneById(o.adminId))
                   .getOrElse(DBIO.successful(None))
          adminResponse = admin.map(StoreAdminResponse.build)
        } yield GiftCardResponse.build(giftCard, None, adminResponse)

      case (GiftCard.AccountPurchase, Some(accountId)) ⇒
        Users.findOneByAccountId(accountId).map { maybeCustomer ⇒
          val customerResponse = maybeCustomer.map(c ⇒ CustomerResponse.build(c))
          GiftCardResponse.build(giftCard, customerResponse, None)
        }

      case _ ⇒ DBIO.successful(GiftCardResponse.build(giftCard, None, None))
    }

  def createByAdmin(admin: StoreAdmin, payload: GiftCardCreateByCsr)(implicit ec: EC,
                                                                     db: DB,
                                                                     ac: AC): DbResultT[Root] =
    for {
      _ ← * <~ payload.validate
      _ ← * <~ Reasons.mustFindById400(payload.reasonId)
      // If `subTypeId` is absent, don't query. Check for model existence otherwise.
      subtype ← * <~ payload.subTypeId.fold(DbResultT.none[GiftCardSubtype]) { subId ⇒
                 GiftCardSubtypes.csrAppeasements
                   .filter(_.id === subId)
                   .mustFindOneOr(NotFoundFailure400(GiftCardSubtype, subId))
                   .map(Some(_)) // A bit silly but need to rewrap it back
               }
      origin ← * <~ GiftCardManuals.create(
                  GiftCardManual(adminId = admin.id, reasonId = payload.reasonId))
      giftCard ← * <~ GiftCards.create(GiftCard.buildAppeasement(payload, origin.id))
      adminResp = Some(StoreAdminResponse.build(admin))
      _ ← * <~ LogActivity.gcCreated(admin, giftCard)
    } yield build(gc = giftCard, admin = adminResp)

  def createBulkByAdmin(admin: StoreAdmin, payload: GiftCardBulkCreateByCsr)(
      implicit ec: EC,
      db: DB,
      ac: AC): DbResultT[Seq[ItemResult]] =
    for {
      _ ← * <~ payload.validate
      gcCreatePayload = GiftCardCreateByCsr(balance = payload.balance,
                                            reasonId = payload.reasonId,
                                            currency = payload.currency)
      response ← * <~ (1 to payload.quantity).value.map { num ⇒
                  createByAdmin(admin, gcCreatePayload).value.map(buildItemResult(_)).toXor
                }
    } yield response

  def bulkUpdateStateByCsr(payload: GiftCardBulkUpdateStateByCsr, admin: StoreAdmin)(
      implicit ec: EC,
      db: DB,
      ac: AC): DbResultT[Seq[ItemResult]] =
    for {
      _ ← * <~ payload.validate.toXor
      response ← * <~ payload.codes.map { code ⇒
                  val itemPayload = GiftCardUpdateStateByCsr(payload.state, payload.reasonId)
                  updateStateByCsr(code, itemPayload, admin).value
                    .map(buildItemResult(_, Some(code)))
                    .toXor
                }
    } yield response

  def updateStateByCsr(code: String, payload: GiftCardUpdateStateByCsr, admin: StoreAdmin)(
      implicit ec: EC,
      db: DB,
      ac: AC): DbResultT[Root] =
    for {
      _        ← * <~ payload.validate
      _        ← * <~ payload.reasonId.map(id ⇒ Reasons.mustFindById400(id)).getOrElse(DbResultT.unit)
      giftCard ← * <~ GiftCards.mustFindByCode(code)
      updated  ← * <~ cancelOrUpdate(giftCard, payload.state, payload.reasonId, admin)
      _        ← * <~ LogActivity.gcUpdated(admin, giftCard, payload)
    } yield GiftCardResponse.build(updated)

  private def cancelOrUpdate(giftCard: GiftCard,
                             newState: GiftCard.State,
                             reasonId: Option[Int],
                             admin: StoreAdmin)(implicit ec: EC) = newState match {
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
