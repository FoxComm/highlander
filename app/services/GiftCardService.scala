package services

import scala.concurrent.{ExecutionContext, Future}

import cats.implicits._
import models.GiftCard.Canceled
import models.GiftCardSubtypes.scope._
import models.activity.ActivityContext
import models.{Customers, GiftCard, GiftCardAdjustments, GiftCardManual, GiftCardManuals, GiftCardSubtype,
GiftCardSubtypes, GiftCards, Reasons, StoreAdmin, StoreAdmins}
import payloads.{GiftCardCreateByCsr, GiftCardUpdateStateByCsr}
import responses.GiftCardBulkResponse._
import responses.GiftCardResponse._
import responses.{TheResponse, CustomerResponse, GiftCardResponse, GiftCardSubTypesResponse, StoreAdminResponse}
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives.SortAndPage
import utils.DbResultT
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick._
import utils.Slick.implicits._

object GiftCardService {
  val mockCustomerId = 1

  type QuerySeq = GiftCards.QuerySeq

  def getOriginTypes
  (implicit db: Database, ec: ExecutionContext): Result[Seq[GiftCardSubTypesResponse.Root]] = (for {

    subTypes ← * <~ GiftCardSubtypes.result.toXor
    response ← * <~ GiftCardSubTypesResponse.build(GiftCard.OriginType.types.toSeq, subTypes)
  } yield response).runTxn()

  def findAll(implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): Result[TheResponse[Seq[Root]]] =
    GiftCards.queryAll.result.map(_.map(GiftCardResponse.build(_))).toTheResponse.run()

  def findByCode(code: String)
    (implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): ResultWithMetadata[Seq[Root]] =
    GiftCards.queryByCode(code).result.map(_.map(GiftCardResponse.build(_)))

  def getByCode(code: String)(implicit db: Database, ec: ExecutionContext): Result[Root] = (for {
    giftCard ← * <~ GiftCards.mustFindByCode(code)
    response ← * <~ buildResponse(giftCard).toXor
  } yield response).run()

  private def buildResponse(giftCard: GiftCard)(implicit ec: ExecutionContext) = giftCard.originType match {
    case GiftCard.CsrAppeasement ⇒ for {
      origin ← GiftCardManuals.filter(_.id === giftCard.originId).one
      admin ← origin.map(o ⇒ StoreAdmins.findOneById(o.adminId)).getOrElse(DBIO.successful(None))
      adminResponse = admin.map(StoreAdminResponse.build)
    } yield GiftCardResponse.build(giftCard, None, adminResponse)

    case GiftCard.CustomerPurchase ⇒ Customers.findOneById(mockCustomerId).map { maybeCustomer ⇒
      val customerResponse = maybeCustomer.map(c ⇒ CustomerResponse.build(c))
      GiftCardResponse.build(giftCard, customerResponse, None)
    }

    case _ ⇒ DBIO.successful(GiftCardResponse.build(giftCard, None, None))
  }

  def createByAdmin(admin: StoreAdmin, payload: payloads.GiftCardCreateByCsr)
    (implicit ec: ExecutionContext, db: Database, ac: ActivityContext): Result[Root] = (for {
    _        ← * <~ payload.validate
    _        ← * <~ Reasons.mustFindById400(payload.reasonId)
    // If `subTypeId` is absent, don't query. Check for model existence otherwise.
    subtype  ← * <~ payload.subTypeId.fold(DbResult.none[GiftCardSubtype]) { subId ⇒
                      GiftCardSubtypes.csrAppeasements.filter(_.id === subId).one
                        .mustFindOr(NotFoundFailure400(GiftCardSubtype, subId))
                        .map(_.map(Some(_))) // A bit silly but need to rewrap it back
                    }
    origin   ← * <~ GiftCardManuals.create(GiftCardManual(adminId = admin.id, reasonId = payload.reasonId))
    giftCard ← * <~ GiftCards.create(GiftCard.buildAppeasement(payload, origin.id))
    adminResp = Some(StoreAdminResponse.build(admin))
    _        ← * <~ LogActivity.gcCreated(admin, giftCard)
  } yield build(gc = giftCard, admin = adminResp)).runTxn()

  def createBulkByAdmin(admin: StoreAdmin, payload: payloads.GiftCardBulkCreateByCsr)
    (implicit ec: ExecutionContext, db: Database, ac: ActivityContext): Result[Seq[ItemResult]] = (for {
    _        ← ResultT.fromXor(payload.validate.toXor)
    gcCreatePayload = GiftCardCreateByCsr(balance = payload.balance, reasonId = payload.reasonId, currency = payload.currency)
    response ← ResultT.right(Future.sequence((1 to payload.quantity).map { num ⇒
                 createByAdmin(admin, gcCreatePayload).map(buildItemResult(_))
               }))
  } yield response).value

  def bulkUpdateStateByCsr(payload: payloads.GiftCardBulkUpdateStateByCsr, admin: StoreAdmin)
    (implicit ec: ExecutionContext, db: Database, ac: ActivityContext): Result[Seq[ItemResult]] = (for {
    _        ← ResultT.fromXor(payload.validate.toXor)
    response ← ResultT.right(Future.sequence(payload.codes.map { code ⇒
                 val itemPayload = GiftCardUpdateStateByCsr(payload.state, payload.reasonId)
                 updateStateByCsr(code, itemPayload, admin).map(buildItemResult(_, Some(code)))
               }))
  } yield response).value

  def updateStateByCsr(code: String, payload: payloads.GiftCardUpdateStateByCsr, admin: StoreAdmin)
    (implicit ec: ExecutionContext, db: Database, ac: ActivityContext): Result[Root] = (for {
    _        ← * <~ payload.validate
    _        ← * <~ payload.reasonId.map(id ⇒ Reasons.mustFindById400(id)).getOrElse(DbResult.unit)
    giftCard ← * <~ GiftCards.mustFindByCode(code)
    updated  ← * <~ cancelOrUpdate(giftCard, payload.state, payload.reasonId, admin)
    _        ← * <~ LogActivity.gcUpdated(admin, giftCard, payload)
  } yield GiftCardResponse.build(updated)).runTxn()

  private def cancelOrUpdate(giftCard: GiftCard, newState: GiftCard.State, reasonId: Option[Int],
    admin: StoreAdmin)(implicit ec: ExecutionContext, db: Database) = newState match {
    case Canceled ⇒ for {
      _   ← * <~ GiftCardAdjustments.lastAuthByGiftCardId(giftCard.id).one.mustNotFindOr(OpenTransactionsFailure)
      upd ← * <~ GiftCards.update(giftCard, giftCard.copy(state = newState, canceledReason = reasonId,
                   canceledAmount = giftCard.availableBalance.some))
      _   ← * <~ GiftCards.cancelByCsr(giftCard, admin)
    } yield upd

    case newState ⇒ DbResultT(GiftCards.update(giftCard, giftCard.copy(state = newState)))
  }
}
