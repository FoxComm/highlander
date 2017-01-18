package services

import failures.CartFailures._
import failures.OrderFailures.OrderLineItemNotFound
import failures.ProductFailures.{ProductVariantNotFoundForContext, ProductVariantNotFoundForContextAndId}
import models.account._
import models.activity.Activity
import models.cord._
import models.cord.lineitems.CartLineItems.scope._
import models.cord.lineitems._
import models.inventory.{ProductVariant, ProductVariants}
import models.objects._
import models.product.ProductValueVariantLinks
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

    val logActivity = (cart: CartResponse, oldQtys: Map[ObjectForm#Id, Int]) ⇒
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

    val logActivity = (cart: CartResponse, oldQtys: Map[ObjectForm#Id, Int]) ⇒
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

    val logActivity = (cart: CartResponse, oldQtys: Map[ObjectForm#Id, Int]) ⇒
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

    val logActivity = (cart: CartResponse, oldQtys: Map[ObjectForm#Id, Int]) ⇒
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
                         logAct: (CartResponse, Map[ObjectForm#Id, Int]) ⇒ DbResultT[Activity])(
      implicit ec: EC,
      es: ES,
      db: DB,
      ctx: OC,
      au: AU): DbResultT[TheResponse[CartResponse]] =
    for {
      _     ← * <~ CartPromotionUpdater.readjust(cart).recover { case _ ⇒ Unit }
      cart  ← * <~ CartTotaler.saveTotals(cart)
      valid ← * <~ CartValidator(cart).validate()
      res   ← * <~ CartResponse.buildRefreshed(cart)
      li    ← * <~ CartLineItems.byCordRef(cart.refNum).countProductVariants
      _     ← * <~ logAct(res, li)
    } yield TheResponse.validated(res, valid)

  def foldQuantityPayload(payload: Seq[UpdateLineItemsPayload]): Map[ObjectForm#Id, Int] =
    payload.groupBy(_.productVariantId).mapValues(_.map(_.quantity).sum)

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
      productVariant ← * <~ ProductVariants
                        .filterByContext(ctx.id)
                        .filter(_.formId === lineItem.productVariantId)
                        .mustFindOneOr(
                            ProductVariantNotFoundForContextAndId(lineItem.productVariantId,
                                                                  ctx.id))
      _ ← * <~ mustFindProductIdForVariant(productVariant, cart.refNum)
      updateResult ← * <~ createLineItems(productVariant.id,
                                          lineItem.quantity,
                                          cart.refNum,
                                          lineItem.attributes)
    } yield updateResult

  private def createLineItems(productVariantId: Int,
                              quantity: Int,
                              cordRef: String,
                              attributes: Option[LineItemAttributes])(implicit ec: EC) = {
    require(quantity > 0)
    val lineItem =
      CartLineItem(cordRef = cordRef, productVariantId = productVariantId, attributes = attributes)
    CartLineItems.createAllReturningModels(List.fill(quantity)(lineItem))
  }

  private def addQuantities(cart: Cart, payload: Seq[UpdateLineItemsPayload])(
      implicit ec: EC,
      ctx: OC): DbResultT[Unit] = {
    val lineItemUpdActions = payload.map { lineItem ⇒
      for {
        productVariant ← * <~ ProductVariants
                          .filterByContext(ctx.id)
                          .filter(_.formId === lineItem.productVariantId)
                          .mustFindOneOr(ProductVariantNotFoundForContext(
                                  s"form-id:${lineItem.productVariantId}",
                                  ctx.id))
        _ ← * <~ mustFindProductIdForVariant(productVariant, cart.refNum)
        _ ← * <~ (if (lineItem.quantity > 0)
                    createLineItems(productVariant.id,
                                    lineItem.quantity,
                                    cart.refNum,
                                    lineItem.attributes).meh
                  else
                    removeLineItems(productVariant.id,
                                    -lineItem.quantity,
                                    cart.refNum,
                                    lineItem.attributes))
      } yield {}
    }
    DbResultT.sequence(lineItemUpdActions).meh
  }

  private def mustFindProductIdForVariant(productVariant: ProductVariant,
                                          refNum: String)(implicit ec: EC, oc: OC) = {
    for {
      link ← * <~ ProductVariantLinks
              .filter(_.rightId === productVariant.id)
              .one
              .dbresult
              .flatMap {
                case Some(productLink) ⇒
                  DbResultT.good(productLink.leftId)
                case None ⇒
                  for {
                    valueLink ← * <~ ProductValueVariantLinks
                                 .filter(_.rightId === productVariant.id)
                                 .mustFindOneOr(SkuWithNoProductAdded(refNum, productVariant.code))
                    variantLink ← * <~ ProductOptionValueLinks
                                   .filter(_.rightId === valueLink.leftId)
                                   .mustFindOneOr(
                                       SkuWithNoProductAdded(refNum, productVariant.code))
                    productLink ← * <~ ProductOptionLinks
                                   .filter(_.rightId === variantLink.leftId)
                                   .mustFindOneOr(
                                       SkuWithNoProductAdded(refNum, productVariant.code))
                  } yield productLink.leftId
              }
    } yield link
  }

  private def removeLineItems(
      productVariantId: Int,
      delta: Int,
      cordRef: String,
      requestedAttrs: Option[LineItemAttributes])(implicit ec: EC): DbResultT[Unit] =
    CartLineItems
      .byCordRef(cordRef)
      .filter(_.productVariantId === productVariantId)
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
