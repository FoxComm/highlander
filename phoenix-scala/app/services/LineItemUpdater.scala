package services

import failures.ProductFailures.SkuNotFoundForContext
import models.activity.Activity
import models.cord._
import models.cord.lineitems.OrderLineItems.scope._
import models.cord.lineitems._
import models.location._
import CartLineItems.scope._
import failures.CartFailures.SKUWithNoProductAdded
import failures.GeneralFailure
import models.account._
import models.inventory.Skus
import models.objects.ProductSkuLinks
import models.payment.giftcard._
import payloads.LineItemPayloads.UpdateLineItemsPayload
import responses.TheResponse
import responses.cord.CartResponse
import services.carts.{CartPromotionUpdater, CartTotaler}
import services.taxes.TaxesService
import slick.driver.PostgresDriver.api._
import utils.apis.Apis
import utils.aliases._
import utils.db._

object LineItemUpdater {

  def updateQuantitiesOnCart(admin: User, refNum: String, payload: Seq[UpdateLineItemsPayload])(
      implicit ec: EC,
      es: ES,
      db: DB,
      ac: AC,
      ctx: OC,
      apis: Apis): DbResultT[TheResponse[CartResponse]] = {

    val logActivity = (cart: CartResponse, oldQtys: Map[String, Int]) ⇒
      LogActivity.orderLineItemsUpdated(cart, oldQtys, payload, Some(admin))

    for {
      cart     ← * <~ Carts.mustFindByRefNum(refNum)
      _        ← * <~ updateQuantities(cart, payload, ctx.id)
      response ← * <~ runUpdates(cart, logActivity)
    } yield response
  }

  def updateQuantitiesOnCustomersCart(customer: User, payload: Seq[UpdateLineItemsPayload])(
      implicit ec: EC,
      es: ES,
      db: DB,
      ac: AC,
      ctx: OC,
      apis: Apis): DbResultT[TheResponse[CartResponse]] = {

    val logActivity = (cart: CartResponse, oldQtys: Map[String, Int]) ⇒
      LogActivity.orderLineItemsUpdated(cart, oldQtys, payload)

    val finder = Carts
      .findByAccountId(customer.accountId)
      .one
      .findOrCreate(Carts.create(Cart(accountId = customer.accountId)))

    for {
      cart     ← * <~ finder
      _        ← * <~ updateQuantities(cart, payload, ctx.id)
      response ← * <~ runUpdates(cart, logActivity)
    } yield response
  }

  def addQuantitiesOnCart(admin: User, refNum: String, payload: Seq[UpdateLineItemsPayload])(
      implicit ec: EC,
      es: ES,
      db: DB,
      ac: AC,
      ctx: OC,
      apis: Apis): DbResultT[TheResponse[CartResponse]] = {

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
      apis: Apis): DbResultT[TheResponse[CartResponse]] = {

    val logActivity = (cart: CartResponse, oldQtys: Map[String, Int]) ⇒
      LogActivity.orderLineItemsUpdated(cart, oldQtys, payload)

    val finder = Carts
      .findByAccountId(customer.accountId)
      .one
      .findOrCreate(Carts.create(Cart(accountId = customer.accountId)))

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
      apis: Apis): DbResultT[TheResponse[CartResponse]] =
    for {
      _           ← * <~ CartPromotionUpdater.readjust(cart).recover { case _ ⇒ Unit }
      li          ← * <~ CartLineItems.byCordRef(cart.refNum).countSkus
      tax         ← * <~ TaxesService.getTaxRate(cart)
      updatedCart ← * <~ CartTotaler.saveTotals(cart, tax)
      valid       ← * <~ CartValidator(updatedCart).validate()
      res         ← * <~ CartResponse.buildRefreshed(updatedCart)
      li          ← * <~ CartLineItems.byCordRef(updatedCart.refNum).countSkus
      _           ← * <~ logAct(res, li)
    } yield TheResponse.validated(res, valid)

  def foldQuantityPayload(payload: Seq[UpdateLineItemsPayload]): Map[String, Int] =
    payload.foldLeft(Map[String, Int]()) { (acc, item) ⇒
      val quantity = acc.getOrElse(item.sku, 0)
      acc.updated(item.sku, quantity + item.quantity)
    }

  private def updateQuantities(cart: Cart, payload: Seq[UpdateLineItemsPayload], contextId: Int)(
      implicit ec: EC): DbResultT[Seq[CartLineItem]] = {

    val lineItemUpdActions = foldQuantityPayload(payload).map {
      case (skuCode, qty) ⇒
        for {
          sku ← * <~ Skus
                 .filterByContext(contextId)
                 .filter(_.code === skuCode)
                 .mustFindOneOr(SkuNotFoundForContext(skuCode, contextId))
          _ ← * <~ ProductSkuLinks
               .filter(_.rightId === sku.id)
               .mustFindOneOr(SKUWithNoProductAdded(cart.refNum, skuCode))
          lis ← * <~ doUpdateLineItems(sku.id, qty, cart.refNum)
        } yield lis
    }
    DbResultT.sequence(lineItemUpdActions).map(_.flatten.toSeq)
  }

  private def addQuantities(cart: Cart, payload: Seq[UpdateLineItemsPayload])(
      implicit ec: EC,
      ctx: OC): DbResultT[Seq[Unit]] = {

    val lineItemUpdActions = foldQuantityPayload(payload).map {
      case (skuCode, delta) ⇒
        for {
          sku ← * <~ Skus
                 .filterByContext(ctx.id)
                 .filter(_.code === skuCode)
                 .mustFindOneOr(SkuNotFoundForContext(skuCode, ctx.id))
          _ ← * <~ ProductSkuLinks
               .filter(_.rightId === sku.id)
               .mustFindOneOr(SKUWithNoProductAdded(cart.refNum, skuCode))
          lis ← * <~ (if (delta > 0) increaseLineItems(sku.id, delta, cart.refNum)
                      else decreaseLineItems(sku.id, -delta, cart.refNum))
        } yield lis
    }
    DbResultT.sequence(lineItemUpdActions).map(_.toSeq)
  }

  private def doUpdateLineItems(skuId: Int, newQuantity: Int, cordRef: String)(
      implicit ec: EC): DbResultT[Seq[CartLineItem]] =
    for {
      current ← * <~ CartLineItems.byCordRef(cordRef).filter(_.skuId === skuId).size.result
      _ ← * <~ (if (newQuantity > current)
                  increaseLineItems(skuId, newQuantity - current, cordRef)
                else decreaseLineItems(skuId, current - newQuantity, cordRef))
      lineItems ← * <~ CartLineItems.byCordRef(cordRef).result
    } yield lineItems

  private def increaseLineItems(skuId: Int, delta: Int, cordRef: String)(
      implicit ec: EC): DbResultT[Unit] = {

    val itemsToInsert: List[CartLineItem] =
      List.fill(delta)(CartLineItem(cordRef = cordRef, skuId = skuId))
    CartLineItems.createAll(itemsToInsert).meh
  }

  private def decreaseLineItems(skuId: Int, delta: Int, cordRef: String)(
      implicit ec: EC): DbResultT[Unit] = {

    val items = CartLineItems.byCordRef(cordRef).filter(_.skuId === skuId)
    items.filter(_.id in items.take(delta).map(_.id)).deleteAll(DbResultT.unit, DbResultT.unit)
  }
}
