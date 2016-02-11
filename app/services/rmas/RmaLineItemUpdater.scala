package services.rmas

import models.{GiftCard, GiftCards, OrderLineItemGiftCards, OrderLineItemSkus, 
  RmaLineItem, RmaLineItemGiftCard, RmaLineItemGiftCards, RmaLineItemShippingCost, 
  RmaLineItemShippingCosts, RmaLineItemSku, RmaLineItemSkus, RmaLineItems, 
  RmaReason, RmaReasons, Rmas, Shipments}
import models.product.{Sku, Skus}
import payloads.{RmaGiftCardLineItemsPayload, RmaShippingCostLineItemsPayload, RmaSkuLineItemsPayload}
import responses.RmaResponse
import responses.RmaResponse.Root
import services.RmaFailures.SkuNotFoundInOrder
import services.rmas.Helpers._
import services.{NotFoundFailure400, NotFoundFailure404, Result, ShipmentNotFoundFailure}
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick._
import utils.Slick.implicits._

import scala.concurrent.ExecutionContext
import slick.driver.PostgresDriver.api._

object RmaLineItemUpdater {

  // FIXME: Fetch reasons with `mustFindOneById`, cc @anna
  def addSkuLineItem(refNum: String, payload: RmaSkuLineItemsPayload)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = (for {
      // Checks
      payload   ← * <~ payload.validate
      rma       ← * <~ mustFindPendingRmaByRefNum(refNum)
      reason    ← * <~ RmaReasons.filter(_.id === payload.reasonId)
        .one.mustFindOr(NotFoundFailure400(RmaReason, payload.reasonId))
      oli       ← * <~ OrderLineItemSkus.join(Skus)
        .one.mustFindOr(SkuNotFoundInOrder(payload.sku, rma.orderRefNum))
      // Inserts
      origin    ← * <~ RmaLineItemSkus.create(RmaLineItemSku(rmaId = rma.id, skuId = oli._2.id))
      li        ← * <~ RmaLineItems.create(RmaLineItem.buildSku(rma, reason, origin, payload))
      // Response
      updated   ← * <~ Rmas.refresh(rma).toXor
      response  ← * <~ RmaResponse.fromRma(updated).toXor
    } yield response).runTxn()

  def deleteSkuLineItem(refNum: String, lineItemId: Int)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = (for {
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

  def addGiftCardLineItem(refNum: String, payload: RmaGiftCardLineItemsPayload)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = (for {
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

  def deleteGiftCardLineItem(refNum: String, lineItemId: Int)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = (for {
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

  def addShippingCostItem(refNum: String, payload: RmaShippingCostLineItemsPayload)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = (for {
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

  def deleteShippingCostLineItem(refNum: String, lineItemId: Int)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = (for {
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
