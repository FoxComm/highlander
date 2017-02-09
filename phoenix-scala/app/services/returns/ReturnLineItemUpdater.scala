package services.returns

import failures.ReturnFailures.ReturnReasonNotFoundFailure
import failures._
import models.objects._
import models.returns._
import models.shipping.Shipments
import payloads.ReturnPayloads._
import responses.ReturnResponse
import services.inventory.SkuManager
import services.returns.Helpers._
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object ReturnLineItemUpdater {

  def addLineItem(refNum: String, payload: ReturnLineItemPayload)(
      implicit ec: EC,
      db: DB,
      oc: OC): DbResultT[ReturnResponse.Root] =
    for {
      rma ← * <~ mustFindPendingReturnByRefNum(refNum)
      reason ← * <~ ReturnReasons
                .filter(_.id === payload.reasonId)
                .mustFindOneOr(ReturnReasonNotFoundFailure(payload.reasonId))
      _        ← * <~ processAddLineItem(rma, reason, payload)
      updated  ← * <~ Returns.refresh(rma)
      response ← * <~ ReturnResponse.fromRma(updated)
    } yield response

  private def processAddLineItem(
      rma: Return,
      reason: ReturnReason,
      payload: ReturnLineItemPayload)(implicit ec: EC, db: DB, oc: OC) = {
    payload match {
      case giftCard: ReturnGiftCardLineItemPayload     ⇒ addGiftCardItem(rma, reason, giftCard)
      case shipping: ReturnShippingCostLineItemPayload ⇒ addShippingCostItem(rma, reason, shipping)
      case sku: ReturnSkuLineItemPayload               ⇒ addSkuLineItem(rma, reason, sku)
    }
  }

  private def addGiftCardItem(
      rma: Return,
      reason: ReturnReason,
      payload: ReturnGiftCardLineItemPayload)(implicit ec: EC, db: DB): DbResultT[ReturnLineItem] =
    ??? // TODO add gift card handling

  private def addShippingCostItem(rma: Return,
                                  reason: ReturnReason,
                                  payload: ReturnShippingCostLineItemPayload)(
      implicit ec: EC,
      db: DB): DbResultT[ReturnLineItem] =
    for {
      shipment ← * <~ Shipments
                  .filter(_.cordRef === rma.orderRef)
                  .mustFindOneOr(ShipmentNotFoundFailure(rma.orderRef))
      origin ← * <~ ReturnLineItemShippingCosts.create(
                  ReturnLineItemShippingCost(returnId = rma.id, shipmentId = shipment.id))
      li ← * <~ ReturnLineItems.create(ReturnLineItem.buildShippinCost(rma, reason, origin))
    } yield li

  private def addSkuLineItem(rma: Return, reason: ReturnReason, payload: ReturnSkuLineItemPayload)(
      implicit ec: EC,
      db: DB,
      oc: OC): DbResultT[ReturnLineItem] =
    for {
      payload   ← * <~ payload.validate
      sku       ← * <~ SkuManager.mustFindSkuByContextAndCode(oc.id, payload.sku)
      skuShadow ← * <~ ObjectShadows.mustFindById404(sku.shadowId)
      origin ← * <~ ReturnLineItemSkus.create(
                  ReturnLineItemSku(returnId = rma.id, skuId = sku.id, skuShadowId = skuShadow.id))
      li ← * <~ ReturnLineItems.create(ReturnLineItem.buildSku(rma, reason, origin, payload))
    } yield li

  def deleteLineItem(refNum: String, lineItemId: Int)(implicit ec: EC,
                                                      db: DB): DbResultT[ReturnResponse.Root] =
    for {
      rma      ← * <~ mustFindPendingReturnByRefNum(refNum)
      li       ← * <~ ReturnLineItems.mustFindById404(lineItemId)
      _        ← * <~ processDeleteLineItem(li, li.originType)
      _        ← * <~ ReturnLineItems.filter(_.id === lineItemId).deleteAll
      updated  ← * <~ Returns.refresh(rma)
      response ← * <~ ReturnResponse.fromRma(updated)
    } yield response

  private def processDeleteLineItem(
      lineItem: ReturnLineItem,
      originType: ReturnLineItem.OriginType)(implicit ec: EC, db: DB): DbResultT[Unit] =
    originType match {
      case ReturnLineItem.GiftCardItem ⇒ deleteGiftCardLineItem(lineItem)
      case ReturnLineItem.ShippingCost ⇒ deleteShippingCostLineItem(lineItem)
      case ReturnLineItem.SkuItem      ⇒ deleteSkuLineItem(lineItem)
    }

  private def deleteGiftCardLineItem(lineItem: ReturnLineItem)(implicit ec: EC,
                                                               db: DB): DbResultT[Unit] =
    ReturnLineItemGiftCards.filter(_.id === lineItem.originId).deleteAll.meh

  private def deleteShippingCostLineItem(lineItem: ReturnLineItem)(implicit ec: EC,
                                                                   db: DB): DbResultT[Unit] =
    ReturnLineItemShippingCosts.filter(_.id === lineItem.originId).deleteAll.meh

  private def deleteSkuLineItem(lineItem: ReturnLineItem)(implicit ec: EC,
                                                          db: DB): DbResultT[Unit] =
    ReturnLineItemSkus.filter(_.id === lineItem.originId).deleteAll.meh
}
