package services.returns

import failures._
import models.cord.lineitems._
import models.objects._
import models.payment.giftcard._
import models.returns._
import models.shipping.Shipments
import payloads.ReturnPayloads._
import responses.ReturnResponse
import responses.ReturnResponse.Root
import services.inventory.SkuManager
import services.returns.Helpers._
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object ReturnLineItemUpdater {

  // FIXME: Fetch reasons with `mustFindOneById`, cc @anna
  def addSkuLineItem(refNum: String, payload: ReturnSkuLineItemsPayload, context: ObjectContext)(
      implicit ec: EC,
      db: DB): DbResultT[Root] =
    for {
      // Checks
      payload ← * <~ payload.validate
      rma     ← * <~ mustFindPendingReturnByRefNum(refNum)
      reason ← * <~ ReturnReasons
                .filter(_.id === payload.reasonId)
                .mustFindOneOr(NotFoundFailure400(ReturnReason, payload.reasonId))
      sku       ← * <~ SkuManager.mustFindSkuByContextAndCode(context.id, payload.sku)
      skuShadow ← * <~ ObjectShadows.findById(sku.shadowId)
      // Inserts
      origin ← * <~ ReturnLineItemSkus.create(
                  ReturnLineItemSku(returnId = rma.id, skuId = sku.id, skuShadowId = skuShadow.id))
      li ← * <~ ReturnLineItems.create(ReturnLineItem.buildSku(rma, reason, origin, payload))
      // Response
      updated  ← * <~ Returns.refresh(rma)
      response ← * <~ ReturnResponse.fromRma(updated)
    } yield response

  def deleteSkuLineItem(refNum: String, lineItemId: Int)(implicit ec: EC,
                                                         db: DB): DbResultT[Root] =
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
    } yield response)

  def addShippingCostItem(refNum: String, payload: ReturnShippingCostLineItemsPayload)(
      implicit ec: EC,
      db: DB): DbResultT[Root] =
    for {
      // Checks
      rma ← * <~ mustFindPendingReturnByRefNum(refNum)
      reason ← * <~ ReturnReasons
                .filter(_.id === payload.reasonId)
                .mustFindOneOr(NotFoundFailure400(ReturnReason, payload.reasonId))
      shipment ← * <~ Shipments
                  .filter(_.cordRef === rma.orderRef)
                  .mustFindOneOr(ShipmentNotFoundFailure(rma.orderRef))
      // Inserts
      origin ← * <~ ReturnLineItemShippingCosts.create(
                  ReturnLineItemShippingCost(returnId = rma.id, shipmentId = shipment.id))
      li ← * <~ ReturnLineItems.create(ReturnLineItem.buildShippinCost(rma, reason, origin))
      // Response
      updated  ← * <~ Returns.refresh(rma)
      response ← * <~ ReturnResponse.fromRma(updated)
    } yield response

  def deleteShippingCostLineItem(refNum: String, lineItemId: Int)(implicit ec: EC,
                                                                  db: DB): DbResultT[Root] =
    for {
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
    } yield response
}
