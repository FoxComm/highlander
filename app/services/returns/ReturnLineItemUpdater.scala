package services.returns

import failures._
import models.objects._
import models.order.lineitems._
import models.payment.giftcard._
import models.returns._
import models.shipping.Shipments
import payloads.ReturnPayloads._
import responses.ReturnResponse
import responses.ReturnResponse.Root
import services.Result
import services.inventory.SkuManager
import services.returns.Helpers._
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object ReturnLineItemUpdater {

  // FIXME: Fetch reasons with `mustFindOneById`, cc @anna
  def addSkuLineItem(refNum: String, payload: ReturnSkuLineItemsPayload, context: ObjectContext)(
      implicit ec: EC,
      db: DB): Result[Root] =
    (for {
      // Checks
      payload ← * <~ payload.validate
      rma     ← * <~ mustFindPendingReturnByRefNum(refNum)
      reason ← * <~ ReturnReasons
                .filter(_.id === payload.reasonId)
                .mustFindOneOr(NotFoundFailure400(ReturnReason, payload.reasonId))
      sku       ← * <~ SkuManager.mustFindSkuByContextAndCode(context.id, payload.sku)
      skuShadow ← * <~ ObjectShadows.mustFindById404(sku.shadowId)
      // Inserts
      origin ← * <~ ReturnLineItemSkus.create(
                  ReturnLineItemSku(returnId = rma.id, skuId = sku.id, skuShadowId = skuShadow.id))
      li ← * <~ ReturnLineItems.create(ReturnLineItem.buildSku(rma, reason, origin, payload))
      // Response
      updated  ← * <~ Returns.refresh(rma)
      response ← * <~ ReturnResponse.fromRma(updated)
    } yield response).runTxn()

  def deleteSkuLineItem(refNum: String, lineItemId: Int)(implicit ec: EC, db: DB): Result[Root] =
    (for {
      // Checks
      rma ← * <~ mustFindPendingReturnByRefNum(refNum)
      lineItem ← * <~ ReturnLineItems
                  .join(ReturnLineItemSkus)
                  .on(_.originId === _.id)
                  .filter { case (oli, sku) ⇒ oli.returnId === rma.id && oli.id === lineItemId }
                  .mustFindOneOr(NotFoundFailure400(ReturnLineItem, lineItemId))
      // Deletes
      _ ← * <~ ReturnLineItems.filter(_.id === lineItemId).delete
      _ ← * <~ ReturnLineItemSkus.filter(_.id === lineItem._2.id).delete
      // Response
      updated  ← * <~ Returns.refresh(rma)
      response ← * <~ ReturnResponse.fromRma(updated)
    } yield response).runTxn()

  def addGiftCardLineItem(refNum: String, payload: ReturnGiftCardLineItemsPayload)(
      implicit ec: EC,
      db: DB): Result[Root] =
    (for {
      // Checks
      rma ← * <~ mustFindPendingReturnByRefNum(refNum)
      reason ← * <~ ReturnReasons
                .filter(_.id === payload.reasonId)
                .mustFindOneOr(NotFoundFailure400(ReturnReason, payload.reasonId))
      oli ← * <~ OrderLineItemGiftCards
             .join(GiftCards)
             .on(_.giftCardId === _.id)
             .filter { case (oli, gc) ⇒ oli.orderRef === rma.orderRef && gc.code === payload.code }
             .mustFindOneOr(NotFoundFailure404(GiftCard, payload.code))
      // Inserts
      origin ← * <~ ReturnLineItemGiftCards.create(
                  ReturnLineItemGiftCard(returnId = rma.id, giftCardId = oli._2.id))
      li ← * <~ ReturnLineItems.create(ReturnLineItem.buildGiftCard(rma, reason, origin))
      // Response
      updated  ← * <~ Returns.refresh(rma)
      response ← * <~ ReturnResponse.fromRma(updated)
    } yield response).runTxn()

  def deleteGiftCardLineItem(refNum: String, lineItemId: Int)(implicit ec: EC,
                                                              db: DB): Result[Root] =
    (for {
      // Checks
      rma ← * <~ mustFindPendingReturnByRefNum(refNum)
      lineItem ← * <~ ReturnLineItems
                  .join(ReturnLineItemGiftCards)
                  .on(_.originId === _.id)
                  .filter { case (oli, sku) ⇒ oli.returnId === rma.id && oli.id === lineItemId }
                  .mustFindOneOr(NotFoundFailure400(ReturnLineItem, lineItemId))
      // Deletes
      _ ← * <~ ReturnLineItems.filter(_.id === lineItemId).delete
      _ ← * <~ ReturnLineItemGiftCards.filter(_.id === lineItem._2.id).delete
      // Response
      updated  ← * <~ Returns.refresh(rma)
      response ← * <~ ReturnResponse.fromRma(updated)
    } yield response).runTxn()

  def addShippingCostItem(refNum: String, payload: ReturnShippingCostLineItemsPayload)(
      implicit ec: EC,
      db: DB): Result[Root] =
    (for {
      // Checks
      rma ← * <~ mustFindPendingReturnByRefNum(refNum)
      reason ← * <~ ReturnReasons
                .filter(_.id === payload.reasonId)
                .mustFindOneOr(NotFoundFailure400(ReturnReason, payload.reasonId))
      shipment ← * <~ Shipments
                  .filter(_.orderRef === rma.orderRef)
                  .mustFindOneOr(ShipmentNotFoundFailure(rma.orderRef))
      // Inserts
      origin ← * <~ ReturnLineItemShippingCosts.create(
                  ReturnLineItemShippingCost(returnId = rma.id, shipmentId = shipment.id))
      li ← * <~ ReturnLineItems.create(ReturnLineItem.buildShippinCost(rma, reason, origin))
      // Response
      updated  ← * <~ Returns.refresh(rma)
      response ← * <~ ReturnResponse.fromRma(updated)
    } yield response).runTxn()

  def deleteShippingCostLineItem(refNum: String, lineItemId: Int)(implicit ec: EC,
                                                                  db: DB): Result[Root] =
    (for {
      // Checks
      rma ← * <~ mustFindPendingReturnByRefNum(refNum)
      lineItem ← * <~ ReturnLineItems
                  .join(ReturnLineItemShippingCosts)
                  .on(_.originId === _.id)
                  .filter { case (oli, sku) ⇒ oli.returnId === rma.id && oli.id === lineItemId }
                  .mustFindOneOr(NotFoundFailure400(ReturnLineItem, lineItemId))
      // Deletes
      _ ← * <~ ReturnLineItems.filter(_.id === lineItemId).delete
      _ ← * <~ ReturnLineItemShippingCosts.filter(_.id === lineItem._2.id).delete
      // Response
      updated  ← * <~ Returns.refresh(rma)
      response ← * <~ ReturnResponse.fromRma(updated)
    } yield response).runTxn()
}
