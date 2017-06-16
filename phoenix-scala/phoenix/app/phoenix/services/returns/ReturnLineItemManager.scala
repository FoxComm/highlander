package phoenix.services.returns

import cats.implicits._
import core.db._
import objectframework.FormShadowGet
import objectframework.models._
import phoenix.failures.ReturnFailures._
import phoenix.models.cord.Orders
import phoenix.models.cord.lineitems.OrderLineItems
import phoenix.models.cord.lineitems.OrderLineItems.scope._
import phoenix.models.returns.ReturnLineItem.OriginType
import phoenix.models.returns._
import phoenix.models.shipping.ShippingMethods
import phoenix.payloads.ReturnPayloads._
import phoenix.responses.ReturnResponse
import phoenix.responses.cord.base.CordResponseLineItems
import phoenix.services.inventory.SkuManager
import phoenix.services.{LineItemManager, LogActivity}
import phoenix.utils.aliases._
import slick.jdbc.PostgresProfile.api._

object ReturnLineItemManager {

  def addLineItem(refNum: String, payload: ReturnLineItemPayload)(implicit ec: EC,
                                                                  db: DB,
                                                                  ac: AC,
                                                                  oc: OC): DbResultT[ReturnResponse.Root] =
    for {
      rma      ← * <~ Returns.mustFindActiveByRefNum404(refNum)
      reason   ← * <~ ReturnReasons.mustFindById400(payload.reasonId)
      _        ← * <~ processAddLineItem(rma, reason, payload)
      updated  ← * <~ Returns.refresh(rma)
      response ← * <~ ReturnResponse.fromRma(updated)
    } yield response

  def updateSkuLineItems(refNum: String, payload: List[ReturnSkuLineItemPayload])(
      implicit ec: EC,
      db: DB,
      ac: AC,
      oc: OC): DbResultT[ReturnResponse.Root] =
    for {
      rma ← * <~ Returns.mustFindActiveByRefNum404(refNum)
      skusLiQuery = ReturnLineItems
        .findByRmaId(rma.id)
        .filter(_.originType === (ReturnLineItem.SkuItem: ReturnLineItem.OriginType))
      skus ← * <~ skusLiQuery
              .join(ReturnLineItemSkus)
              .on(_.id === _.id)
              .map { case (_, sku) ⇒ sku }
              .to[List]
              .result
      _        ← * <~ skusLiQuery.deleteAll
      _        ← * <~ doOrMeh(skus.nonEmpty, LogActivity().returnSkuLineItemsDropped(skus))
      _        ← * <~ payload.map(p ⇒ ReturnReasons.mustFindById400(p.reasonId).flatMap(addSkuLineItem(rma, _, p)))
      updated  ← * <~ Returns.refresh(rma)
      response ← * <~ ReturnResponse.fromRma(updated)
    } yield response

  private def processAddLineItem(rma: Return,
                                 reason: ReturnReason,
                                 payload: ReturnLineItemPayload)(implicit ec: EC, db: DB, ac: AC, oc: OC) =
    payload match {
      case shipping: ReturnShippingCostLineItemPayload ⇒ addShippingCostItem(rma, reason, shipping)
      case sku: ReturnSkuLineItemPayload               ⇒ addSkuLineItem(rma, reason, sku)
    }

  private def validateMaxShippingCost(rma: Return, amount: Long)(implicit ec: EC, db: DB): DbResultT[Unit] =
    for {
      order ← * <~ Orders.mustFindByRefNum(rma.orderRef)
      orderShippingTotal = order.shippingTotal
      previouslyReturnedCost ← * <~ Returns
                                .findPrevious(rma)
                                .join(ReturnLineItemShippingCosts)
                                .on(_.id === _.returnId)
                                .map { case (_, shippingCost) ⇒ shippingCost.amount }
                                .sum
                                .getOrElse(0L)
                                .result
      maxAmount = orderShippingTotal - previouslyReturnedCost
      _ ← * <~ failIf(
           amount > maxAmount,
           ReturnShippingCostExceeded(refNum = rma.referenceNumber, amount = amount, maxAmount = maxAmount))
    } yield ()

  private def addShippingCostItem(rma: Return,
                                  reason: ReturnReason,
                                  payload: ReturnShippingCostLineItemPayload)(
      implicit ec: EC,
      ac: AC,
      db: DB): DbResultT[ReturnLineItem] =
    for {
      _ ← * <~ validateMaxShippingCost(rma, payload.amount)
      oli ← * <~ ReturnLineItems
             .findByRmaId(rma.id)
             .filter(_.originType === (ReturnLineItem.ShippingCost: OriginType))
             .one
      _ ← * <~ oli.map(li ⇒ ReturnLineItems.filter(_.id === li.id).deleteAll)
      li ← * <~ ReturnLineItems.create(
            ReturnLineItem(
              returnId = rma.id,
              reasonId = reason.id,
              originType = ReturnLineItem.ShippingCost
            ))
      _ ← * <~ ReturnLineItemShippingCosts.create(
           ReturnLineItemShippingCost(id = li.id, returnId = rma.id, amount = payload.amount))
      _ ← * <~ LogActivity().returnShippingCostItemAdded(rma, reason, payload)
    } yield li

  private def validateMaxQuantity(rma: Return, sku: String, quantity: Int)(implicit ec: EC,
                                                                           db: DB,
                                                                           oc: OC): DbResultT[Unit] =
    for {
      order ← * <~ Orders.mustFindByRefNum(rma.orderRef)
      orderedQuantity ← * <~ OrderLineItems
                         .findByOrderRef(rma.orderRef)
                         .forContextAndCode(oc.id, sku)
                         .distinct
                         .length
                         .result
      previouslyReturned ← * <~ Returns
                            .findPrevious(rma)
                            .join(ReturnLineItemSkus.findByContextAndCode(oc.id, sku))
                            .on(_.id === _.returnId)
                            .distinct
                            .length
                            .result
      maxQuantity = orderedQuantity - previouslyReturned
      _ ← * <~ failIf(quantity > maxQuantity,
                      ReturnSkuItemQuantityExceeded(refNum = rma.referenceNumber,
                                                    quantity = quantity,
                                                    maxQuantity = maxQuantity))
    } yield ()

  private def addSkuLineItem(
      rma: Return,
      reason: ReturnReason,
      payload: ReturnSkuLineItemPayload)(implicit ec: EC, db: DB, ac: AC, oc: OC): DbResultT[ReturnLineItem] =
    for {
      _         ← * <~ validateMaxQuantity(rma, payload.sku, payload.quantity)
      sku       ← * <~ SkuManager.mustFindSkuByContextAndCode(oc.id, payload.sku)
      skuShadow ← * <~ ObjectShadows.mustFindById404(sku.shadowId)
      oli ← * <~ ReturnLineItemSkus
             .findByContextAndCode(oc.id, payload.sku)
             .join(ReturnLineItems.findByRmaId(rma.id))
             .on(_.id === _.id)
             .map { case (_, rli) ⇒ rli }
             .one
      _ ← * <~ oli.map(li ⇒ ReturnLineItems.filter(_.id === li.id).deleteAll)
      li ← * <~ ReturnLineItems.create(
            ReturnLineItem(
              returnId = rma.id,
              reasonId = reason.id,
              originType = ReturnLineItem.SkuItem
            ))
      _ ← * <~ ReturnLineItemSkus.create(
           ReturnLineItemSku(id = li.id,
                             returnId = rma.id,
                             quantity = payload.quantity,
                             skuId = sku.id,
                             skuShadowId = skuShadow.id))
      _ ← * <~ LogActivity().returnSkuLineItemAdded(rma, reason, payload)
    } yield li

  def deleteLineItem(refNum: String,
                     lineItemId: Int)(implicit ec: EC, ac: AC, db: DB): DbResultT[ReturnResponse.Root] =
    for {
      rma     ← * <~ Returns.mustFindActiveByRefNum404(refNum)
      li      ← * <~ ReturnLineItems.mustFindById404(lineItemId)
      deleted ← * <~ ReturnLineItems.filter(_.id === li.id).deleteAllWithRowsBeingAffected
      _ ← * <~ doOrMeh(
           deleted,
           li.originType match {
             case ReturnLineItem.ShippingCost ⇒ LogActivity().returnShippingCostItemDeleted(li)
             case ReturnLineItem.SkuItem      ⇒ LogActivity().returnSkuLineItemDeleted(li)
           }
         )
      updated  ← * <~ Returns.refresh(rma)
      response ← * <~ ReturnResponse.fromRma(updated)
    } yield response

  def fetchSkuLineItems(rma: Return)(implicit ec: EC, db: DB): DbResultT[Seq[ReturnResponse.LineItem.Sku]] = {
    val skusQuery = (for {
      liSku  ← ReturnLineItemSkus.findByRmaId(rma.id)
      li     ← ReturnLineItems if liSku.id === li.id
      reason ← li.returnReason
      sku    ← liSku.sku
      shadow ← liSku.shadow
      form   ← ObjectForms if form.id === sku.formId
    } yield (li.id, reason.name, liSku.quantity, sku, form, shadow)).result

    for {
      skus ← * <~ skusQuery
      skus ← * <~ skus.toStream.traverse {
              case v @ (_, _, _, sku, _, _) ⇒ LineItemManager.getLineItemImage(sku).map(_ → v)
            }
    } yield
      skus.flatMap {
        case (image, (id, reason, quantity, sku, form, shadow)) ⇒
          for {
            (price, currency) ← FormShadowGet.price(form, shadow)
            title             ← FormShadowGet.title(form, shadow)
          } yield
            ReturnResponse.LineItem.Sku(
              id = id,
              reason = reason,
              imagePath = image.getOrElse(CordResponseLineItems.NO_IMAGE),
              title = title,
              sku = sku.code,
              quantity = quantity,
              price = price,
              currency = currency
            )
      }
  }

  def fetchShippingCostLineItem(
      rma: Return)(implicit ec: EC, db: DB): DbResultT[Option[ReturnResponse.LineItem.ShippingCost]] = {
    val shippingCostsQuery = (for {
      liSc   ← ReturnLineItemShippingCosts.findByRmaId(rma.id)
      li     ← liSc.li
      reason ← li.returnReason
    } yield (liSc, li, reason)).one.dbresult

    for {
      shippingCosts ← * <~ shippingCostsQuery
      // FIXME: ShippingMethod has only price attached, without any currency. Also, what with EOM here?
      order          ← * <~ Orders.mustFindByRefNum(rma.orderRef)
      shippingMethod ← * <~ ShippingMethods.forCordRef(rma.orderRef).one
    } yield
      shippingCosts.map2(shippingMethod) {
        case ((costs, li, reason), sm) ⇒
          ReturnResponse.LineItem.ShippingCost(id = li.id,
                                               reason = reason.name,
                                               name = sm.adminDisplayName,
                                               amount = costs.amount,
                                               price = sm.price,
                                               currency = order.currency)
      }
  }
}
