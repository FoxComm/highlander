package services

import failures.ProductFailures.SkuNotFoundForContext
import models.StoreAdmin
import models.activity.Activity
import models.cord._
import models.cord.lineitems.OrderLineItems.scope._
import models.cord.lineitems._
import CartLineItems.scope._
import models.customer.Customer
import models.inventory.Skus
import models.payment.giftcard._
import payloads.LineItemPayloads.UpdateLineItemsPayload
import responses.TheResponse
import responses.cord.CartResponse
import services.carts.{CartPromotionUpdater, CartTotaler}
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object LineItemUpdater {

  def updateQuantitiesOnCart(admin: StoreAdmin,
                             refNum: String,
                             payload: Seq[UpdateLineItemsPayload])(
      implicit ec: EC,
      es: ES,
      db: DB,
      ac: AC,
      ctx: OC): DbResultT[TheResponse[CartResponse]] = {

    val logActivity = (cart: CartResponse, oldQtys: Map[String, Int]) ⇒
      LogActivity.orderLineItemsUpdated(cart, oldQtys, payload, Some(admin))

    for {
      cart     ← * <~ Carts.mustFindByRefNum(refNum)
      _        ← * <~ updateQuantities(cart, payload, ctx.id)
      response ← * <~ runUpdates(cart, logActivity)
    } yield response
  }

  def updateQuantitiesOnCustomersCart(customer: Customer, payload: Seq[UpdateLineItemsPayload])(
      implicit ec: EC,
      es: ES,
      db: DB,
      ac: AC,
      ctx: OC): DbResultT[TheResponse[CartResponse]] = {

    val findOrCreate = Carts
      .findByCustomer(customer)
      .one
      .findOrCreateExtended(Carts.create(Cart(customerId = customer.id)))

    val logActivity = (cart: CartResponse, oldQtys: Map[String, Int]) ⇒
      LogActivity.orderLineItemsUpdated(cart, oldQtys, payload)

    val finder = findOrCreate.map({ case (cart, _) ⇒ cart })

    for {
      cart     ← * <~ finder
      _        ← * <~ updateQuantities(cart, payload, ctx.id)
      response ← * <~ runUpdates(cart, logActivity)
    } yield response
  }

  def addQuantitiesOnCart(admin: StoreAdmin, refNum: String, payload: Seq[UpdateLineItemsPayload])(
      implicit ec: EC,
      es: ES,
      db: DB,
      ac: AC,
      ctx: OC): DbResultT[TheResponse[CartResponse]] = {

    val logActivity = (cart: CartResponse, oldQtys: Map[String, Int]) ⇒
      LogActivity.orderLineItemsUpdated(cart, oldQtys, payload, Some(admin))

    for {
      cart     ← * <~ Carts.mustFindByRefNum(refNum)
      _        ← * <~ addQuantities(cart, payload, ctx.id)
      response ← * <~ runUpdates(cart, logActivity)
    } yield response
  }

  def addQuantitiesOnCustomersCart(customer: Customer, payload: Seq[UpdateLineItemsPayload])(
      implicit ec: EC,
      es: ES,
      db: DB,
      ac: AC,
      ctx: OC): DbResultT[TheResponse[CartResponse]] = {

    val findOrCreate = Carts
      .findByCustomer(customer)
      .one
      .findOrCreateExtended(Carts.create(Cart(customerId = customer.id)))

    val logActivity = (cart: CartResponse, oldQtys: Map[String, Int]) ⇒
      LogActivity.orderLineItemsUpdated(cart, oldQtys, payload)

    val finder = findOrCreate.map({ case (cart, _) ⇒ cart })

    for {
      cart     ← * <~ finder
      _        ← * <~ addQuantities(cart, payload, ctx.id)
      response ← * <~ runUpdates(cart, logActivity)
    } yield response
  }

  private def runUpdates(cart: Cart,
                         logAct: (CartResponse, Map[String, Int]) ⇒ DbResultT[Activity])(
      implicit ec: EC,
      es: ES,
      db: DB,
      ctx: OC): DbResultT[TheResponse[CartResponse]] =
    for {
      _     ← * <~ CartPromotionUpdater.readjust(cart).recover { case _ ⇒ Unit }
      cart  ← * <~ CartTotaler.saveTotals(cart)
      valid ← * <~ CartValidator(cart).validate()
      res   ← * <~ CartResponse.buildRefreshed(cart)
      li    ← * <~ CartLineItems.byCordRef(cart.refNum).countSkus
      _     ← * <~ logAct(res, li)
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
          lis ← * <~ doUpdateLineItems(sku.id, qty, cart.refNum)
        } yield lis
    }
    DbResultT.sequence(lineItemUpdActions).map(_.flatten.toSeq)
  }

  private def addQuantities(cart: Cart, payload: Seq[UpdateLineItemsPayload], contextId: Int)(
      implicit ec: EC): DbResultT[Seq[Unit]] = {

    val lineItemUpdActions = foldQuantityPayload(payload).map {
      case (skuCode, delta) ⇒
        for {
          sku ← * <~ Skus
                 .filterByContext(contextId)
                 .filter(_.code === skuCode)
                 .mustFindOneOr(SkuNotFoundForContext(skuCode, contextId))
          lis ← * <~ (if (delta > 0) increaseLineItems(sku.id, delta, cart.refNum)
                      else decreaseLineItems(sku.id, -delta, cart.refNum))
        } yield lis
    }
    DbResultT.sequence(lineItemUpdActions).map(_.toSeq)
  }

  private def doUpdateLineItems(skuId: Int, newQuantity: Int, cordRef: String)(
      implicit ec: EC): DbResultT[Seq[CartLineItem]] = {

    val allRelatedLineItems = CartLineItems.byCordRef(cordRef).filter(_.skuId === skuId)

    val counts = allRelatedLineItems.size.result.toXor

    counts.flatMap { (current: Int) ⇒
      // we're using absolute values from payload, so if newQuantity is greater then create N items
      if (newQuantity > current) {
        val itemsToInsert: List[CartLineItem] =
          List.fill(newQuantity - current)(CartLineItem(cordRef = cordRef, skuId = skuId))
        CartLineItems.createAll(itemsToInsert).ignoreResult
      } else if (current - newQuantity > 0) {

        // otherwise delete N items
        val queries = for {
          deleteLi ← allRelatedLineItems
                      .filter(_.id in allRelatedLineItems.take(current - newQuantity).map(_.id))
                      .delete
        } yield ()

        DbResultT.fromDbio(queries)
      } else {
        DbResultT.unit
      }
    }.flatMap { _ ⇒
      DbResultT.fromDbio(CartLineItems.byCordRef(cordRef).result)
    }
  }

  private def increaseLineItems(skuId: Int, delta: Int, cordRef: String)(
      implicit ec: EC): DbResultT[Unit] = {

    val itemsToInsert: List[CartLineItem] =
      List.fill(delta)(CartLineItem(cordRef = cordRef, skuId = skuId))
    CartLineItems.createAll(itemsToInsert).ignoreResult
  }

  private def decreaseLineItems(skuId: Int, delta: Int, cordRef: String)(
      implicit ec: EC): DbResultT[Unit] = {

    val query = CartLineItems.byCordRef(cordRef).filter(_.skuId === skuId)

    for {
      itemCount ← * <~ query.size.result
      qty = if (itemCount > delta) delta else itemCount
      deletedQty ← * <~ query.filter(_.id in query.take(qty).map(_.id)).delete
    } yield {}
  }
}
