package services

import failures.ProductFailures.SkuNotFoundForContext
import models.StoreAdmin
import models.activity.Activity
import models.cord._
import models.cord.lineitems.OrderLineItems.scope._
import models.cord.lineitems._
import models.customer.Customer
import models.inventory.Skus
import models.payment.giftcard._
import payloads.LineItemPayloads.{AddGiftCardLineItem, UpdateLineItemsPayload}
import responses.TheResponse
import responses.cart.FullCart
import services.carts.{CartPromotionUpdater, CartTotaler}
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object LineItemUpdater {

  def addGiftCard(admin: StoreAdmin, refNum: String, payload: AddGiftCardLineItem)(
      implicit ec: EC,
      db: DB,
      ac: AC,
      ctx: OC): Result[TheResponse[FullCart.Root]] =
    (for {
      p      ← * <~ payload.validate
      cart   ← * <~ Carts.mustFindByRefNum(refNum)
      origin ← * <~ GiftCardOrders.create(GiftCardOrder(cordRef = cart.refNum))
      gc ← * <~ GiftCards.create(GiftCard
                .buildLineItem(balance = p.balance, originId = origin.id, currency = p.currency))
      liGc ← * <~ OrderLineItemGiftCards.create(
                OrderLineItemGiftCard(giftCardId = gc.id, cordRef = cart.refNum))
      _ ← * <~ OrderLineItems.create(OrderLineItem.buildGiftCard(cart, liGc))
      // update changed totals
      cart   ← * <~ CartTotaler.saveTotals(cart)
      valid  ← * <~ CartValidator(cart).validate()
      result ← * <~ FullCart.buildRefreshed(cart)
      _      ← * <~ LogActivity.orderLineItemsAddedGc(admin, result, gc)
    } yield TheResponse.build(result, alerts = valid.alerts, warnings = valid.warnings)).runTxn()

  def editGiftCard(admin: StoreAdmin, refNum: String, code: String, payload: AddGiftCardLineItem)(
      implicit ec: EC,
      db: DB,
      ac: AC,
      ctx: OC): Result[TheResponse[FullCart.Root]] =
    (for {
      _        ← * <~ payload.validate
      giftCard ← * <~ GiftCards.mustFindByCode(code)
      _        ← * <~ giftCard.mustBeCart
      cart     ← * <~ Carts.mustFindByRefNum(refNum)
      _        ← * <~ GiftCards.filter(_.id === giftCard.id).update(GiftCard.update(giftCard, payload))
      // update changed totals
      cart   ← * <~ CartTotaler.saveTotals(cart)
      valid  ← * <~ CartValidator(cart).validate()
      result ← * <~ FullCart.buildRefreshed(cart)
      _      ← * <~ LogActivity.orderLineItemsUpdatedGc(admin, result, giftCard)
    } yield TheResponse.build(result, alerts = valid.alerts, warnings = valid.warnings)).runTxn()

  def deleteGiftCard(
      admin: StoreAdmin,
      refNum: String,
      code: String)(implicit ec: EC, db: DB, ac: AC, ctx: OC): Result[TheResponse[FullCart.Root]] =
    (for {
      gc   ← * <~ GiftCards.mustFindByCode(code)
      _    ← * <~ gc.mustBeCart
      cart ← * <~ Carts.mustFindByRefNum(refNum)
      _    ← * <~ OrderLineItemGiftCards.findByGcId(gc.id).delete
      // FIXME @anna WTF? Order id or GC id?
      _ ← * <~ OrderLineItems.filter(_.cordRef === cart.refNum).giftCards.delete
      _ ← * <~ GiftCards.filter(_.id === gc.id).delete
      _ ← * <~ GiftCardOrders.filter(_.id === gc.originId).delete
      // update changed totals
      cart  ← * <~ CartTotaler.saveTotals(cart)
      valid ← * <~ CartValidator(cart).validate()
      res   ← * <~ FullCart.buildRefreshed(cart)
      _     ← * <~ LogActivity.orderLineItemsDeletedGc(admin, res, gc)
    } yield TheResponse.build(res, alerts = valid.alerts, warnings = valid.warnings)).runTxn()

  def updateQuantitiesOnCart(admin: StoreAdmin,
                             refNum: String,
                             payload: Seq[UpdateLineItemsPayload])(
      implicit ec: EC,
      es: ES,
      db: DB,
      ac: AC,
      ctx: OC): Result[TheResponse[FullCart.Root]] = {

    val finder = Carts.mustFindByRefNum(refNum)
    val logActivity = (cart: FullCart.Root, oldQtys: Map[String, Int]) ⇒
      LogActivity.orderLineItemsUpdated(cart, oldQtys, payload, Some(admin))

    runUpdates(finder, logActivity, payload)
  }

  def updateQuantitiesOnCustomersCart(customer: Customer, payload: Seq[UpdateLineItemsPayload])(
      implicit ec: EC,
      es: ES,
      db: DB,
      ac: AC,
      ctx: OC): Result[TheResponse[FullCart.Root]] = {

    val findOrCreate = Carts
      .findByCustomer(customer)
      .one
      .findOrCreateExtended(Carts.create(Cart(customerId = customer.id)))

    val logActivity = (cart: FullCart.Root, oldQtys: Map[String, Int]) ⇒
      LogActivity.orderLineItemsUpdated(cart, oldQtys, payload)

    val finder = findOrCreate.map({ case (cart, _) ⇒ cart })

    runUpdates(finder, logActivity, payload)
  }

  private def runUpdates(finder: DbResultT[Cart],
                         logAct: (FullCart.Root, Map[String, Int]) ⇒ DbResultT[Activity],
                         payload: Seq[UpdateLineItemsPayload])(
      implicit ec: EC,
      es: ES,
      db: DB,
      ctx: OC): Result[TheResponse[FullCart.Root]] =
    (for {
      cart ← * <~ finder
      // load old line items for activity trail
      li ← * <~ OrderLineItemSkus.findLineItemsByCordRef(cart.refNum).result
      lineItems = li.foldLeft(Map[String, Int]()) {
        case (acc, (sku, form, shadow, _, _)) ⇒
          val quantity = acc.getOrElse(sku.code, 0)
          acc.updated(sku.code, quantity + 1)
      }
      // update quantities
      _ ← * <~ updateQuantities(cart, payload, ctx.id)
      // update changed totals
      _     ← * <~ CartPromotionUpdater.readjust(cart).recover { case _ ⇒ Unit }
      cart  ← * <~ CartTotaler.saveTotals(cart)
      valid ← * <~ CartValidator(cart).validate()
      res   ← * <~ FullCart.buildRefreshed(cart)
      _     ← * <~ logAct(res, lineItems)
    } yield TheResponse.build(res, alerts = valid.alerts, warnings = valid.warnings)).runTxn()

  def foldQuantityPayload(payload: Seq[UpdateLineItemsPayload]): Map[String, Int] =
    payload.foldLeft(Map[String, Int]()) { (acc, item) ⇒
      val quantity = acc.getOrElse(item.sku, 0)
      acc.updated(item.sku, quantity + item.quantity)
    }

  private def updateQuantities(cart: Cart, payload: Seq[UpdateLineItemsPayload], contextId: Int)(
      implicit ec: EC): DbResultT[Seq[OrderLineItem]] = {

    val lineItemUpdActions = foldQuantityPayload(payload).map {
      case (skuCode, qty) ⇒
        for {
          sku ← * <~ Skus
                 .filterByContext(contextId)
                 .filter(_.code === skuCode)
                 .mustFindOneOr(SkuNotFoundForContext(skuCode, contextId))
          lis ← * <~ fuckingHell(sku.id, qty, cart.refNum)
        } yield lis
    }
    DbResultT.sequence(lineItemUpdActions).map(_.flatten.toSeq)
  }

  private def fuckingHell(skuId: Int, newQuantity: Int, cordRef: String)(
      implicit ec: EC): DbResultT[Seq[OrderLineItem]] = {
    val counts = for {
      (skuId, q) ← OrderLineItems
                    .filter(_.cordRef === cordRef)
                    .skuItems
                    .join(OrderLineItemSkus)
                    .on(_.originId === _.id)
                    .groupBy(_._2.skuId)
    } yield (skuId, q.length)

    counts.result.toXor.flatMap { (items: Seq[(Int, Int)]) ⇒
      val existingSkuCounts = items.toMap

      val current = existingSkuCounts.getOrElse(skuId, 0)

      // we're using absolute values from payload, so if newQuantity is greater then create N items
      if (newQuantity > current) {
        val delta = newQuantity - current

        val queries = for {
          origin ← OrderLineItemSkus.safeFindBySkuId(skuId)
          // FIXME: should use `createAll` instead of `++=` but this method is a nightmare to refactor
          bulkInsert ← OrderLineItems ++= (1 to delta).map { _ ⇒
                        OrderLineItem(cordRef = cordRef, originId = origin.id)
                      }.toSeq
        } yield ()

        DbResultT.fromDbio(queries)
      } else if (current - newQuantity > 0) {
        // otherwise delete N items
        val queries = for {
          deleteLi ← OrderLineItems
                      .filter(
                          _.id in OrderLineItems
                            .filter(_.cordRef === cordRef)
                            .skuItems
                            .join(OrderLineItemSkus)
                            .on(_.originId === _.id)
                            .filter(_._2.skuId === skuId)
                            .sortBy(_._1.id.asc)
                            .take(current - newQuantity)
                            .map(_._1.id))
                      .delete
        } yield ()

        DbResultT.fromDbio(queries)
      } else {
        DbResultT.unit
      }
    }.flatMap { _ ⇒
      DbResultT.fromDbio(OrderLineItems.filter(_.cordRef === cordRef).result)
    }
  }
}
