package services.returns

import failures.ShippingMethodFailures.ShippingMethodNotFoundInOrder
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
                .mustFindOneOr(NotFoundFailure404(ReturnReason, payload.reasonId))
      _        ← * <~ processAddLineItem(rma, reason, payload)
      updated  ← * <~ Returns.refresh(rma)
      response ← * <~ ReturnResponse.fromRma(updated)
    } yield response

  def processAddLineItem(rma: Return, reason: ReturnReason, payload: ReturnLineItemPayload)(
      implicit ec: EC,
      db: DB,
      oc: OC) = {
    payload match {
      case giftCard: ReturnGiftCardLineItemPayload     ⇒ addGiftCardItem(rma, reason, giftCard)
      case shipping: ReturnShippingCostLineItemPayload ⇒ addShippingCostItem(rma, reason, shipping)
      case sku: ReturnSkuLineItemPayload               ⇒ addSkuLineItem(rma, reason, sku)
    }
  }

  def addGiftCardItem(rma: Return, reason: ReturnReason, payload: ReturnGiftCardLineItemPayload)(
      implicit ec: EC,
      db: DB): DbResultT[ReturnLineItem] = ??? // TODO add gift card handling

  def addShippingCostItem(rma: Return,
                          reason: ReturnReason,
                          payload: ReturnShippingCostLineItemPayload)(
      implicit ec: EC,
      db: DB): DbResultT[ReturnLineItem] =
    for {
      shipment ← * <~ Shipments
                  .filter(_.cordRef === rma.orderRef)
                  .mustFindOneOr(ShippingMethodNotFoundInOrder(rma.orderRef))
      origin ← * <~ ReturnLineItemShippingCosts.create(
                  ReturnLineItemShippingCost(returnId = rma.id, shipmentId = shipment.id))
      li ← * <~ ReturnLineItems.create(ReturnLineItem.buildShippinCost(rma, reason, origin))
    } yield li

  def addSkuLineItem(rma: Return, reason: ReturnReason, payload: ReturnSkuLineItemPayload)(
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

  def processDeleteLineItem(lineItem: ReturnLineItem, originType: ReturnLineItem.OriginType)(
      implicit ec: EC,
      db: DB): DbResultT[Int] = originType match {
    case ReturnLineItem.GiftCardItem ⇒ deleteGiftCardLineItem(lineItem)
    case ReturnLineItem.ShippingCost ⇒ deleteShippingCostLineItem(lineItem)
    case ReturnLineItem.SkuItem      ⇒ deleteSkuLineItem(lineItem)
  }

  def deleteGiftCardLineItem(lineItem: ReturnLineItem)(implicit ec: EC, db: DB): DbResultT[Int] =
    ReturnLineItemGiftCards.filter(_.id === lineItem.originId).deleteAll

  def deleteShippingCostLineItem(lineItem: ReturnLineItem)(implicit ec: EC,
                                                           db: DB): DbResultT[Int] =
    ReturnLineItemShippingCosts.filter(_.id === lineItem.originId).deleteAll

  def deleteSkuLineItem(lineItem: ReturnLineItem)(implicit ec: EC, db: DB): DbResultT[Int] =
    ReturnLineItemSkus.filter(_.id === lineItem.originId).deleteAll
}
