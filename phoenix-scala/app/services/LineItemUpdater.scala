package services

import failures.CartFailures._
import failures.OrderFailures.OrderLineItemNotFound
import failures.ProductFailures.SkuNotFoundForContext
import models.account._
import models.activity.Activity
import models.cord._
import models.cord.lineitems.CartLineItems.scope._
import models.cord.lineitems._
import models.inventory.{Sku, Skus}
import models.objects._
import models.product.VariantValueSkuLinks
import payloads.LineItemPayloads._
import responses.TheResponse
import responses.cord.{CartResponse, OrderResponse}
import services.carts.{CartPromotionUpdater, CartTotaler}
import slick.driver.PostgresDriver.api._
import utils.JsonFormatters
import utils.aliases._
import utils.db._

object LineItemUpdater {

  implicit val formats = JsonFormatters.phoenixFormats

  def updateQuantitiesOnCart(admin: User, refNum: String, payload: Seq[UpdateLineItemsPayload])(
      implicit ec: EC,
      es: ES,
      db: DB,
      ac: AC,
      ctx: OC,
      au: AU): DbResultT[TheResponse[CartResponse]] = {

    val logActivity = (cart: CartResponse, oldQtys: Map[String, Int]) ⇒
      LogActivity.orderLineItemsUpdated(cart, oldQtys, payload, Some(admin))

    for {
      cart     ← * <~ Carts.mustFindByRefNum(refNum)
      _        ← * <~ updateQuantities(cart, payload)
      response ← * <~ runUpdates(cart, logActivity)
    } yield response
  }

  def updateOrderLineItems(admin: User, payload: Seq[UpdateOrderLineItemsPayload], refNum: String)(
      implicit ec: EC,
      es: ES,
      db: DB,
      ac: AC,
      ctx: OC): DbResultT[OrderResponse] =
    for {
      _             ← * <~ runOrderLineItemUpdates(payload)
      orderUpdated  ← * <~ Orders.mustFindByRefNum(refNum)
      orderResponse ← * <~ OrderResponse.fromOrder(orderUpdated, grouped = true)
    } yield orderResponse

  private def runOrderLineItemUpdates(payload: Seq[UpdateOrderLineItemsPayload])(implicit ec: EC,
                                                                                 es: ES,
                                                                                 db: DB,
                                                                                 ac: AC,
                                                                                 ctx: OC) =
    DbResultT.sequence(payload.map(updatePayload ⇒
              for {
        orderLineItem ← * <~ OrderLineItems
                         .filter(_.referenceNumber === updatePayload.referenceNumber)
                         .mustFindOneOr(OrderLineItemNotFound(updatePayload.referenceNumber))
        patch = orderLineItem.copy(state = updatePayload.state,
                                   attributes = updatePayload.attributes)
        updatedItem ← * <~ OrderLineItems.update(orderLineItem, patch)
      } yield updatedItem))

  def updateQuantitiesOnCustomersCart(customer: User, payload: Seq[UpdateLineItemsPayload])(
      implicit ec: EC,
      es: ES,
      db: DB,
      ac: AC,
      ctx: OC,
      au: AU): DbResultT[TheResponse[CartResponse]] = {

    val logActivity = (cart: CartResponse, oldQtys: Map[String, Int]) ⇒
      LogActivity.orderLineItemsUpdated(cart, oldQtys, payload)

    val finder = Carts
      .findByAccountId(customer.accountId)
      .one
      .findOrCreate(Carts.create(Cart(accountId = customer.accountId, scope = Scope.current)))

    for {
      cart     ← * <~ finder
      _        ← * <~ updateQuantities(cart, payload)
      response ← * <~ runUpdates(cart, logActivity)
    } yield response
  }

  def addQuantitiesOnCart(admin: User, refNum: String, payload: Seq[UpdateLineItemsPayload])(
      implicit ec: EC,
      es: ES,
      db: DB,
      ac: AC,
      ctx: OC,
      au: AU): DbResultT[TheResponse[CartResponse]] = {

    val logActivity = (cart: CartResponse, oldQtys: Map[String, Int]) ⇒
      LogActivity.orderLineItemsUpdated(cart, oldQtys, payload, Some(admin))

    for {
      cart     ← * <~ Carts.mustFindByRefNum(refNum)
      _        ← * <~ addQuantities(cart, payload)
      response ← * <~ runUpdates(cart, logActivity)
    } yield response
  }

  def addQuantitiesOnCustomersCart(customer: User, payload: Seq[UpdateLineItemsPayload])(
      implicit ec: EC,
      es: ES,
      db: DB,
      ac: AC,
      ctx: OC,
      au: AU): DbResultT[TheResponse[CartResponse]] = {

    val logActivity = (cart: CartResponse, oldQtys: Map[String, Int]) ⇒
      LogActivity.orderLineItemsUpdated(cart, oldQtys, payload)

    val finder = Carts
      .findByAccountId(customer.accountId)
      .one
      .findOrCreate(Carts.create(Cart(accountId = customer.accountId, scope = Scope.current)))

    for {
      cart     ← * <~ finder
      _        ← * <~ addQuantities(cart, payload)
      response ← * <~ runUpdates(cart, logActivity)
    } yield response
  }

  private def runUpdates(cart: Cart,
                         logAct: (CartResponse, Map[String, Int]) ⇒ DbResultT[Activity])(
      implicit ec: EC,
      es: ES,
      db: DB,
      ctx: OC,
      au: AU): DbResultT[TheResponse[CartResponse]] =
    for {
      readjustedCartWithWarnings ← * <~ CartPromotionUpdater
                                    .readjust(cart, failFatally = false)
                                    .recover {
                                      case _ ⇒ TheResponse(cart) /* FIXME ;( */
                                    }
      cart  ← * <~ CartTotaler.saveTotals(readjustedCartWithWarnings.result)
      valid ← * <~ CartValidator(cart).validate()
      res   ← * <~ CartResponse.buildRefreshed(cart)
      li    ← * <~ CartLineItems.byCordRef(cart.refNum).countSkus
      _     ← * <~ logAct(res, li)
    } yield {
      val blah = TheResponse.validated(res, valid)
      // FIXME: we need a better way to compose TheResult. :s
      blah.copy(warnings = {
        val xs = readjustedCartWithWarnings.warnings.toList.flatten ::: blah.warnings.toList.flatten
        if (xs.isEmpty) None else Some(xs)
      })
    }

  def foldQuantityPayload(payload: Seq[UpdateLineItemsPayload]): Map[String, Int] =
    payload.foldLeft(Map[String, Int]()) { (acc, item) ⇒
      val quantity = acc.getOrElse(item.sku, 0)
      acc.updated(item.sku, quantity + item.quantity)
    }

  private def updateQuantities(cart: Cart, payload: Seq[UpdateLineItemsPayload])(
      implicit ec: EC,
      ctx: OC): DbResultT[Seq[CartLineItem]] =
    for {
      _ ← * <~ CartLineItems
           .byCordRef(cart.referenceNumber)
           .deleteAll(DbResultT.unit, DbResultT.unit)
      updateResult ← * <~ payload.filter(_.quantity > 0).map(updateLineItems(cart, _))
    } yield updateResult.flatten

  private def updateLineItems(cart: Cart, lineItem: UpdateLineItemsPayload)(implicit ec: EC,
                                                                            ctx: OC) =
    for {
      sku ← * <~ Skus
             .filterByContext(ctx.id)
             .filter(_.code === lineItem.sku)
             .mustFindOneOr(SkuNotFoundForContext(lineItem.sku, ctx.id))
      _ ← * <~ mustFindProductIdForSku(sku, cart.refNum)
      updateResult ← * <~ createLineItems(sku.id,
                                          lineItem.quantity,
                                          cart.refNum,
                                          lineItem.attributes)
    } yield updateResult

  private def createLineItems(skuId: Int,
                              quantity: Int,
                              cordRef: String,
                              attributes: Option[LineItemAttributes])(implicit ec: EC) = {
    require(quantity > 0)
    val lineItem = CartLineItem(cordRef = cordRef, skuId = skuId, attributes = attributes)
    CartLineItems.createAllReturningModels(List.fill(quantity)(lineItem))
  }

  private def addQuantities(cart: Cart, payload: Seq[UpdateLineItemsPayload])(
      implicit ec: EC,
      ctx: OC): DbResultT[Unit] = {
    val lineItemUpdActions = payload.map { lineItem ⇒
      for {
        sku ← * <~ Skus
               .filterByContext(ctx.id)
               .filter(_.code === lineItem.sku)
               .mustFindOneOr(SkuNotFoundForContext(lineItem.sku, ctx.id))
        _ ← * <~ mustFindProductIdForSku(sku, cart.refNum)
        _ ← * <~ (if (lineItem.quantity > 0)
                    createLineItems(sku.id, lineItem.quantity, cart.refNum, lineItem.attributes).meh
                  else
                    removeLineItems(sku.id, -lineItem.quantity, cart.refNum, lineItem.attributes))
      } yield {}
    }
    DbResultT.sequence(lineItemUpdActions).meh
  }

  private def mustFindProductIdForSku(sku: Sku, refNum: String)(implicit ec: EC, oc: OC) = {
    for {
      link ← * <~ ProductSkuLinks.filter(_.rightId === sku.id).one.dbresult.flatMap {
              case Some(productLink) ⇒
                DbResultT.good(productLink.leftId)
              case None ⇒
                for {
                  valueLink ← * <~ VariantValueSkuLinks
                               .filter(_.rightId === sku.id)
                               .mustFindOneOr(SkuWithNoProductAdded(refNum, sku.code))
                  variantLink ← * <~ VariantValueLinks
                                 .filter(_.rightId === valueLink.leftId)
                                 .mustFindOneOr(SkuWithNoProductAdded(refNum, sku.code))
                  productLink ← * <~ ProductVariantLinks
                                 .filter(_.rightId === variantLink.leftId)
                                 .mustFindOneOr(SkuWithNoProductAdded(refNum, sku.code))
                } yield productLink.leftId
            }
    } yield link
  }

  private def removeLineItems(
      skuId: Int,
      delta: Int,
      cordRef: String,
      requestedAttrs: Option[LineItemAttributes])(implicit ec: EC): DbResultT[Unit] =
    CartLineItems
      .byCordRef(cordRef)
      .filter(_.skuId === skuId)
      .result
      .dbresult
      .flatMap { lineItemsInCart ⇒
        val lisMatchingPayload = lineItemsInCart.filter(_.attributes == requestedAttrs).map(_.id)

        val totalToDelete = Math.min(delta, lisMatchingPayload.length)
        val idsToDelete   = lisMatchingPayload.take(totalToDelete)

        CartLineItems.filter(_.id.inSet(idsToDelete)).deleteAll(DbResultT.unit, DbResultT.unit)
      }
      .meh
}
