package services

import failures.ProductFailures.SkuNotFoundForContext
import models.account.User
import models.activity.Activity
import models.cord._
import models.cord.lineitems.OrderLineItems.scope._
import models.cord.lineitems._
import CartLineItems.scope._
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

  def updateQuantitiesOnCart(admin: User, refNum: String, payload: Seq[UpdateLineItemsPayload])(
      implicit ec: EC,
      es: ES,
      db: DB,
      ac: AC,
      ctx: OC): DbResultT[TheResponse[CartResponse]] = {

    val finder = Carts.mustFindByRefNum(refNum)
    val logActivity = (cart: CartResponse, oldQtys: Map[String, Int]) ⇒
      LogActivity.orderLineItemsUpdated(cart, oldQtys, payload, Some(admin))

    runUpdates(finder, logActivity, payload)
  }

  def updateQuantitiesOnCustomersCart(customer: User, payload: Seq[UpdateLineItemsPayload])(
      implicit ec: EC,
      es: ES,
      db: DB,
      ac: AC,
      ctx: OC): DbResultT[TheResponse[CartResponse]] = {

    val findOrCreate = Carts
      .findByAccountId(customer.accountId)
      .one
      .findOrCreateExtended(Carts.create(Cart(accountId = customer.accountId)))

    val logActivity = (cart: CartResponse, oldQtys: Map[String, Int]) ⇒
      LogActivity.orderLineItemsUpdated(cart, oldQtys, payload)

    val finder = findOrCreate.map({ case (cart, _) ⇒ cart })

    runUpdates(finder, logActivity, payload)
  }

  private def runUpdates(finder: DbResultT[Cart],
                         logAct: (CartResponse, Map[String, Int]) ⇒ DbResultT[Activity],
                         payload: Seq[UpdateLineItemsPayload])(
      implicit ec: EC,
      es: ES,
      db: DB,
      ctx: OC): DbResultT[TheResponse[CartResponse]] =
    for {
      cart  ← * <~ finder
      _     ← * <~ updateQuantities(cart, payload, ctx.id)
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
}
