package services

import failures.ProductFailures.SkuNotFoundForContext
import models.StoreAdmin
import models.activity.Activity
import models.customer.Customer
import models.inventory.{Sku, Skus}
import models.objects.ObjectContext
import models.order._
import models.order.lineitems.OrderLineItems.scope._
import models.order.lineitems._
import models.payment.giftcard._
import payloads.LineItemPayloads.{AddGiftCardLineItem, UpdateLineItemsPayload}
import responses.TheResponse
import responses.order.FullOrder
import responses.order.FullOrder.refreshAndFullOrder
import services.orders.{OrderPromotionUpdater, OrderTotaler}
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db.DbResultT._
import utils.db._

object LineItemUpdater {
  def addGiftCard(admin: StoreAdmin, refNum: String, payload: AddGiftCardLineItem)(
      implicit ec: EC,
      db: DB,
      ac: AC): Result[TheResponse[FullOrder.Root]] =
    (for {
      p      ← * <~ payload.validate
      order  ← * <~ Orders.mustFindByRefNum(refNum)
      _      ← * <~ order.mustBeCart
      origin ← * <~ GiftCardOrders.create(GiftCardOrder(orderRef = order.refNum))
      gc ← * <~ GiftCards.create(GiftCard
                .buildLineItem(balance = p.balance, originId = origin.id, currency = p.currency))
      liGc ← * <~ OrderLineItemGiftCards.create(
                OrderLineItemGiftCard(giftCardId = gc.id, orderRef = order.refNum))
      _ ← * <~ OrderLineItems.create(OrderLineItem.buildGiftCard(order, liGc))
      // update changed totals
      order  ← * <~ OrderTotaler.saveTotals(order)
      valid  ← * <~ CartValidator(order).validate()
      result ← * <~ refreshAndFullOrder(order).toXor
      _      ← * <~ LogActivity.orderLineItemsAddedGc(admin, result, gc)
    } yield TheResponse.build(result, alerts = valid.alerts, warnings = valid.warnings)).runTxn()

  def editGiftCard(admin: StoreAdmin, refNum: String, code: String, payload: AddGiftCardLineItem)(
      implicit ec: EC,
      db: DB,
      ac: AC): Result[TheResponse[FullOrder.Root]] =
    (for {
      _        ← * <~ payload.validate
      giftCard ← * <~ GiftCards.mustFindByCode(code)
      _        ← * <~ giftCard.mustBeCart
      order    ← * <~ Orders.mustFindByRefNum(refNum)
      _        ← * <~ order.mustBeCart
      _        ← * <~ GiftCards.filter(_.id === giftCard.id).update(GiftCard.update(giftCard, payload))
      // update changed totals
      order  ← * <~ OrderTotaler.saveTotals(order)
      valid  ← * <~ CartValidator(order).validate()
      result ← * <~ refreshAndFullOrder(order).toXor
      _      ← * <~ LogActivity.orderLineItemsUpdatedGc(admin, result, giftCard)
    } yield TheResponse.build(result, alerts = valid.alerts, warnings = valid.warnings)).runTxn()

  def deleteGiftCard(admin: StoreAdmin, refNum: String, code: String)(
      implicit ec: EC,
      db: DB,
      ac: AC): Result[TheResponse[FullOrder.Root]] =
    (for {
      gc    ← * <~ GiftCards.mustFindByCode(code)
      _     ← * <~ gc.mustBeCart
      order ← * <~ Orders.mustFindByRefNum(refNum)
      _     ← * <~ order.mustBeCart
      _     ← * <~ OrderLineItemGiftCards.findByGcId(gc.id).delete
      // FIXME @anna WTF? Order id or GC id?
      _ ← * <~ OrderLineItems.filter(_.orderRef === order.refNum).giftCards.delete
      _ ← * <~ GiftCards.filter(_.id === gc.id).delete
      _ ← * <~ GiftCardOrders.filter(_.id === gc.originId).delete
      // update changed totals
      order ← * <~ OrderTotaler.saveTotals(order)
      valid ← * <~ CartValidator(order).validate()
      res   ← * <~ refreshAndFullOrder(order).toXor
      _     ← * <~ LogActivity.orderLineItemsDeletedGc(admin, res, gc)
    } yield TheResponse.build(res, alerts = valid.alerts, warnings = valid.warnings)).runTxn()

  def updateQuantitiesOnOrder(admin: StoreAdmin,
                              refNum: String,
                              payload: Seq[UpdateLineItemsPayload])(
      implicit ec: EC,
      es: ES,
      db: DB,
      ac: AC): Result[TheResponse[FullOrder.Root]] = {

    val finder = Orders.mustFindByRefNum(refNum)
    val logActivity = (order: FullOrder.Root, oldQtys: Map[String, Int]) ⇒
      LogActivity.orderLineItemsUpdated(order, oldQtys, payload, Some(admin))

    runUpdates(finder, logActivity, payload)
  }

  def updateQuantitiesOnCustomersOrder(customer: Customer,
                                       payload: Seq[UpdateLineItemsPayload],
                                       context: ObjectContext)(
      implicit ec: EC,
      es: ES,
      db: DB,
      ac: AC): Result[TheResponse[FullOrder.Root]] = {

    val findOrCreate = Orders
      .findActiveOrderByCustomer(customer)
      .one
      // TODO @anna: #longlivedbresultt
      .findOrCreateExtended(Orders.create(Order.buildCart(customer.id, context.id)).value)

    val logActivity = (order: FullOrder.Root, oldQtys: Map[String, Int]) ⇒
      LogActivity.orderLineItemsUpdated(order, oldQtys, payload)

    val finder = findOrCreate.map(_.map { case (order, _) ⇒ order })

    runUpdates(finder, logActivity, payload)
  }

  private def runUpdates(finder: DbResult[Order],
                         logAct: (FullOrder.Root, Map[String, Int]) ⇒ DbResult[Activity],
                         payload: Seq[UpdateLineItemsPayload])(
      implicit ec: EC,
      es: ES,
      db: DB): Result[TheResponse[FullOrder.Root]] =
    (for {
      order ← * <~ finder
      _     ← * <~ order.mustBeCart
      // load old line items for activity trail
      li ← * <~ OrderLineItemSkus.findLineItemsByOrder(order).result
      lineItems = li.foldLeft(Map[String, Int]()) {
        case (acc, (sku, form, shadow, _, _)) ⇒
          val quantity = acc.getOrElse(sku.code, 0)
          acc.updated(sku.code, quantity + 1)
      }
      // update quantities
      _ ← * <~ updateQuantities(order, payload)
      // update changed totals
      _     ← * <~ OrderPromotionUpdater.readjust(order).recover { case _ ⇒ Unit }
      order ← * <~ OrderTotaler.saveTotals(order)
      valid ← * <~ CartValidator(order).validate()
      res   ← * <~ refreshAndFullOrder(order).toXor
      _     ← * <~ logAct(res, lineItems)
    } yield TheResponse.build(res, alerts = valid.alerts, warnings = valid.warnings)).runTxn()

  def foldQuantityPayload(payload: Seq[UpdateLineItemsPayload]): Map[String, Int] =
    payload.foldLeft(Map[String, Int]()) { (acc, item) ⇒
      val quantity = acc.getOrElse(item.sku, 0)
      acc.updated(item.sku, quantity + item.quantity)
    }

  private def updateQuantities(order: Order, payload: Seq[UpdateLineItemsPayload])(
      implicit ec: EC): DbResultT[Seq[OrderLineItem]] = {

    val lineItemUpdActions = foldQuantityPayload(payload).map {
      case (skuCode, qty) ⇒
        for {
          sku ← * <~ Skus
                 .filterByContext(order.contextId)
                 .filter(_.code === skuCode)
                 .mustFindOneOr(SkuNotFoundForContext(skuCode, order.contextId))
          lis ← * <~ fuckingHell(sku.id, qty, order.refNum)
        } yield lis
    }
    DbResultT.sequence(lineItemUpdActions).map(_.flatten.toSeq)
  }

  private def fuckingHell(skuId: Int, newQuantity: Int, orderRef: String)(
      implicit ec: EC): DbResult[Seq[OrderLineItem]] = {
    val counts = for {
      (skuId, q) ← OrderLineItems
                    .filter(_.orderRef === orderRef)
                    .skuItems
                    .join(OrderLineItemSkus)
                    .on(_.originId === _.id)
                    .groupBy(_._2.skuId)
    } yield (skuId, q.length)

    counts.result.flatMap { (items: Seq[(Int, Int)]) ⇒
      val existingSkuCounts = items.toMap

      val current = existingSkuCounts.getOrElse(skuId, 0)

      // we're using absolute values from payload, so if newQuantity is greater then create N items
      if (newQuantity > current) {
        val delta = newQuantity - current

        val queries = for {
          origin ← OrderLineItemSkus.safeFindBySkuId(skuId)
          // FIXME: should use `createAll` instead of `++=` but this method is a nightmare to refactor
          bulkInsert ← OrderLineItems ++= (1 to delta).map { _ ⇒
                        OrderLineItem(orderRef = orderRef, originId = origin.id)
                      }.toSeq
        } yield ()

        DbResult.fromDbio(queries)
      } else if (current - newQuantity > 0) {
        // otherwise delete N items
        val queries = for {
          deleteLi ← OrderLineItems
                      .filter(
                          _.id in OrderLineItems
                            .filter(_.orderRef === orderRef)
                            .skuItems
                            .join(OrderLineItemSkus)
                            .on(_.originId === _.id)
                            .filter(_._2.skuId === skuId)
                            .sortBy(_._1.id.asc)
                            .take(current - newQuantity)
                            .map(_._1.id))
                      .delete
        } yield ()

        DbResult.fromDbio(queries)
      } else {
        DbResult.unit
      }
    }.flatMap { _ ⇒
      DbResult.fromDbio(OrderLineItems.filter(_.orderRef === orderRef).result)
    }
  }
}
