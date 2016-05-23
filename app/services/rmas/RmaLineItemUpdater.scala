package services.rmas

import models.inventory.{Sku, Skus}
import models.objects.{ObjectContext, ObjectForms, ObjectShadows}
import models.order.lineitems.{OrderLineItemSkus, OrderLineItemGiftCards}
import models.payment.giftcard.{GiftCards, GiftCard}
import failures.{NotFoundFailure400, NotFoundFailure404, ShipmentNotFoundFailure}
import failures.OrderFailures.SkuNotFoundInOrder
import failures.ProductFailures.SkuNotFoundForContext
import models.rma._
import models.shipping.Shipments
import payloads.RmaPayloads._
import responses.RmaResponse
import responses.RmaResponse.Root
import services.Result
import services.inventory.SkuManager
import services.rmas.Helpers._
import utils.aliases._
import utils.db._
import utils.db.DbResultT._
import slick.driver.PostgresDriver.api._

object RmaLineItemUpdater {

  // FIXME: Fetch reasons with `mustFindOneById`, cc @anna
  def addSkuLineItem(refNum: String, payload: RmaSkuLineItemsPayload, context: ObjectContext)
    (implicit ec: EC, db: DB): Result[Root] = (for {
      // Checks
      payload   ← * <~ payload.validate
      rma       ← * <~ mustFindPendingRmaByRefNum(refNum)
      reason    ← * <~ RmaReasons.filter(_.id === payload.reasonId)
        .one.mustFindOr(NotFoundFailure400(RmaReason, payload.reasonId))
      sku       ← * <~ SkuManager.mustFindSkuByContextAndCode(context.id, payload.sku)
      skuShadow ← * <~ ObjectShadows.mustFindById404(sku.shadowId)
      // Inserts
      origin    ← * <~ RmaLineItemSkus.create(RmaLineItemSku(rmaId = rma.id, skuId = sku.id, skuShadowId = skuShadow.id))
      li        ← * <~ RmaLineItems.create(RmaLineItem.buildSku(rma, reason, origin, payload))
      // Response
      updated   ← * <~ Rmas.refresh(rma).toXor
      response  ← * <~ RmaResponse.fromRma(updated).toXor
    } yield response).runTxn()

  def deleteSkuLineItem(refNum: String, lineItemId: Int)(implicit ec: EC, db: DB): Result[Root] = (for {
    // Checks
    rma       ← * <~ mustFindPendingRmaByRefNum(refNum)
    lineItem  ← * <~ RmaLineItems.join(RmaLineItemSkus).on(_.originId === _.id)
      .filter { case (oli, sku) ⇒ oli.rmaId === rma.id && oli.id === lineItemId }
      .one.mustFindOr(NotFoundFailure400(RmaLineItem, lineItemId))
    // Deletes
    _         ← * <~ RmaLineItems.filter(_.id === lineItemId).delete
    _         ← * <~ RmaLineItemSkus.filter(_.id === lineItem._2.id).delete
    // Response
    updated   ← * <~ Rmas.refresh(rma).toXor
    response  ← * <~ RmaResponse.fromRma(updated).toXor
  } yield response).runTxn()

  def addGiftCardLineItem(refNum: String, payload: RmaGiftCardLineItemsPayload)(implicit ec: EC, db: DB): Result[Root] = (for {
    // Checks
    rma       ← * <~ mustFindPendingRmaByRefNum(refNum)
    reason    ← * <~ RmaReasons.filter(_.id === payload.reasonId)
      .one.mustFindOr(NotFoundFailure400(RmaReason, payload.reasonId))
    oli       ← * <~ OrderLineItemGiftCards.join(GiftCards).on(_.giftCardId === _.id)
      .filter { case (oli, gc) ⇒ oli.orderId === rma.orderId && gc.code === payload.code }
      .one.mustFindOr(NotFoundFailure404(GiftCard, payload.code))
    // Inserts
    origin    ← * <~ RmaLineItemGiftCards.create(RmaLineItemGiftCard(rmaId = rma.id, giftCardId = oli._2.id))
    li        ← * <~ RmaLineItems.create(RmaLineItem.buildGiftCard(rma, reason, origin))
    // Response
    updated   ← * <~ Rmas.refresh(rma).toXor
    response  ← * <~ RmaResponse.fromRma(updated).toXor
  } yield response).runTxn()

  def deleteGiftCardLineItem(refNum: String, lineItemId: Int)(implicit ec: EC, db: DB): Result[Root] = (for {
    // Checks
    rma       ← * <~ mustFindPendingRmaByRefNum(refNum)
    lineItem  ← * <~ RmaLineItems.join(RmaLineItemGiftCards).on(_.originId === _.id)
      .filter { case (oli, sku) ⇒ oli.rmaId === rma.id && oli.id === lineItemId }
      .one.mustFindOr(NotFoundFailure400(RmaLineItem, lineItemId))
    // Deletes
    _         ← * <~ RmaLineItems.filter(_.id === lineItemId).delete
    _         ← * <~ RmaLineItemGiftCards.filter(_.id === lineItem._2.id).delete
    // Response
    updated   ← * <~ Rmas.refresh(rma).toXor
    response  ← * <~ RmaResponse.fromRma(updated).toXor
  } yield response).runTxn()

  def addShippingCostItem(refNum: String, payload: RmaShippingCostLineItemsPayload)(implicit ec: EC, db: DB): Result[Root] = (for {
    // Checks
    rma       ← * <~ mustFindPendingRmaByRefNum(refNum)
    reason    ← * <~ RmaReasons.filter(_.id === payload.reasonId)
      .one.mustFindOr(NotFoundFailure400(RmaReason, payload.reasonId))
    shipment  ← * <~ Shipments.filter(_.orderId === rma.orderId).one.mustFindOr(ShipmentNotFoundFailure(rma.orderRefNum))
    // Inserts
    origin    ← * <~ RmaLineItemShippingCosts.create(RmaLineItemShippingCost(rmaId = rma.id, shipmentId = shipment.id))
    li        ← * <~ RmaLineItems.create(RmaLineItem.buildShippinCost(rma, reason, origin))
    // Response
    updated   ← * <~ Rmas.refresh(rma).toXor
    response  ← * <~ RmaResponse.fromRma(updated).toXor
  } yield response).runTxn()

  def deleteShippingCostLineItem(refNum: String, lineItemId: Int)(implicit ec: EC, db: DB): Result[Root] = (for {
    // Checks
    rma       ← * <~ mustFindPendingRmaByRefNum(refNum)
    lineItem  ← * <~ RmaLineItems.join(RmaLineItemShippingCosts).on(_.originId === _.id)
      .filter { case (oli, sku) ⇒ oli.rmaId === rma.id && oli.id === lineItemId }
      .one.mustFindOr(NotFoundFailure400(RmaLineItem, lineItemId))
    // Deletes
    _         ← * <~ RmaLineItems.filter(_.id === lineItemId).delete
    _         ← * <~ RmaLineItemShippingCosts.filter(_.id === lineItem._2.id).delete
    // Response
    updated   ← * <~ Rmas.refresh(rma).toXor
    response  ← * <~ RmaResponse.fromRma(updated).toXor
  } yield response).runTxn()
}
