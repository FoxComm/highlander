package services

import scala.concurrent.ExecutionContext

import models.OrderLineItems.scope._
import models.{Customer, GiftCard, GiftCardOrder, GiftCardOrders, GiftCards, Order, OrderLineItem,
OrderLineItemGiftCard, OrderLineItemGiftCards, OrderLineItemSku, OrderLineItemSkus, OrderLineItems, Orders, Sku, Skus,
StoreAdmin}
import payloads.{AddGiftCardLineItem, UpdateLineItemsPayload}
import responses.FullOrder.refreshAndFullOrder
import responses.{FullOrder, TheResponse}
import services.orders.OrderTotaler
import slick.driver.PostgresDriver.api._
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick._
import utils.Slick.implicits._

import models.activity.ActivityContext

object LineItemUpdater {
  def addGiftCard(admin: StoreAdmin, refNum: String, payload: AddGiftCardLineItem)
    (implicit ec: ExecutionContext, db: Database, ac: ActivityContext): Result[TheResponse[FullOrder.Root]] = (for {
    p      ← * <~ payload.validate
    order  ← * <~ Orders.mustFindByRefNum(refNum)
    _      ← * <~ order.mustBeCart
    origin ← * <~ GiftCardOrders.create(GiftCardOrder(orderId = order.id))
    gc     ← * <~ GiftCards.create(GiftCard.buildLineItem(balance = p.balance, originId = origin.id, currency = p.currency))
    liGc   ← * <~ OrderLineItemGiftCards.create(OrderLineItemGiftCard(giftCardId = gc.id, orderId = order.id))
    _      ← * <~ OrderLineItems.create(OrderLineItem.buildGiftCard(order, liGc))
    // update changed totals
    order  ← * <~ OrderTotaler.saveTotals(order)
    valid  ← * <~ CartValidator(order).validate
    result ← * <~ refreshAndFullOrder(order).toXor
    _      ← * <~ LogActivity.orderLineItemsAddedGc(admin, result, gc)
  } yield TheResponse.build(result, alerts = valid.alerts, warnings = valid.warnings)).runT()

  def editGiftCard(admin: StoreAdmin, refNum: String, code: String, payload: AddGiftCardLineItem)
    (implicit ec: ExecutionContext, db: Database, ac: ActivityContext): Result[TheResponse[FullOrder.Root]] = (for {
    _        ← * <~ payload.validate
    giftCard ← * <~ GiftCards.mustFindByCode(code)
    _        ← * <~ giftCard.mustBeCart
    order    ← * <~ Orders.mustFindByRefNum(refNum)
    _        ← * <~ order.mustBeCart
    _        ← * <~ GiftCards.filter(_.id === giftCard.id).update(GiftCard.update(giftCard, payload))
    // update changed totals
    order    ← * <~ OrderTotaler.saveTotals(order)
    valid    ← * <~ CartValidator(order).validate
    result   ← * <~ refreshAndFullOrder(order).toXor
    _        ← * <~ LogActivity.orderLineItemsUpdatedGc(admin, result, giftCard)
  } yield TheResponse.build(result, alerts = valid.alerts, warnings = valid.warnings)).runT()

  def deleteGiftCard(admin: StoreAdmin, refNum: String, code: String)
    (implicit ec: ExecutionContext, db: Database, ac: ActivityContext): Result[TheResponse[FullOrder.Root]] = (for {
    gc    ← * <~ GiftCards.mustFindByCode(code)
    _     ← * <~ gc.mustBeCart
    order ← * <~ Orders.mustFindByRefNum(refNum)
    _     ← * <~ order.mustBeCart
    _     ← * <~ OrderLineItemGiftCards.findByGcId(gc.id).delete
    _     ← * <~ OrderLineItems.filter(_.originId === order.id).giftCards.delete
    _     ← * <~ GiftCards.filter(_.id === gc.id).delete
    _     ← * <~ GiftCardOrders.filter(_.id === gc.originId).delete
    // update changed totals
    order ← * <~ OrderTotaler.saveTotals(order)
    valid ← * <~ CartValidator(order).validate
    res   ← * <~ refreshAndFullOrder(order).toXor
    _     ← * <~ LogActivity.orderLineItemsDeletedGc(admin, res, gc)
  } yield TheResponse.build(res, alerts = valid.alerts, warnings = valid.warnings)).runT()

  def updateQuantitiesOnOrder(admin: StoreAdmin, refNum: String, payload: Seq[UpdateLineItemsPayload])
    (implicit ec: ExecutionContext, db: Database, ac: ActivityContext): Result[TheResponse[FullOrder.Root]] = (for {
    order ← * <~ Orders.mustFindByRefNum(refNum)
    _     ← * <~ order.mustBeCart
    _     ← * <~ updateQuantities(order, payload)
    // update changed totals
    order ← * <~ OrderTotaler.saveTotals(order)
    valid ← * <~ CartValidator(order).validate
    res   ← * <~ refreshAndFullOrder(order).toXor
    _     ← * <~ LogActivity.orderLineItemsUpdated(admin, res, payload)
  } yield TheResponse.build(res, alerts = valid.alerts, warnings = valid.warnings)).runT()

  def updateQuantitiesOnCustomersOrder(customer: Customer, payload: Seq[UpdateLineItemsPayload])
    (implicit ec: ExecutionContext, db: Database, ac: ActivityContext): Result[TheResponse[FullOrder.Root]] = {
    def failure = NotFoundFailure404(s"Order with customerId=${customer.id} not found")
    (for {
      order ← * <~ Orders.findActiveOrderByCustomer(customer).one.mustFindOr(failure)
      _     ← * <~ order.mustBeCart
      _     ← * <~ updateQuantities(order, payload)
      // update changed totals
      order ← * <~ OrderTotaler.saveTotals(order)
      valid ← * <~ CartValidator(order).validate
      res   ← * <~ refreshAndFullOrder(order).toXor
      _     ← * <~ LogActivity.orderLineItemsUpdatedByCustomer(customer, res, payload)
    } yield TheResponse.build(res, alerts = valid.alerts, warnings = valid.warnings)).runT()
  }

  private def qtyAvailableForSkus(skus: Seq[String])
    (implicit ec: ExecutionContext, db: Database): DBIO[Map[Sku, Int]] = {

    // TODO: inventory... 'nuff said. (aka FIXME)
    // Skus.qtyAvailableForSkus(updateQuantities.keys.toSeq).flatMap { availableQuantities ⇒
    (for {
      sku ← Skus.filter(_.sku.inSet(skus))
    } yield (sku, 1000000)).result.map(_.toMap)
  }

  private def updateQuantities(order: Order, payload: Seq[UpdateLineItemsPayload])
    (implicit ec: ExecutionContext, db: Database): DbResult[Seq[OrderLineItem]] = {

    val updateQuantities = payload.foldLeft(Map[String, Int]()) { (acc, item) ⇒
      val quantity = acc.getOrElse(item.sku, 0)
      acc.updated(item.sku, quantity + item.quantity)
    }

    qtyAvailableForSkus(updateQuantities.keys.toSeq).flatMap { availableQuantities ⇒
      val enoughOnHand = availableQuantities.foldLeft(Map.empty[Sku, Int]) { case (acc, (sku, numAvailable)) ⇒
        val numRequested = updateQuantities.getOrElse(sku.sku, 0)
        if (numRequested >= 0) acc.updated(sku, numRequested) else acc
          // TODO: reinstate when we have real inventory
//        if (numAvailable >= numRequested && numRequested >= 0)
//          acc.updated(sku, numRequested)
//        else
//          acc
      }

      // select oli_skus.sku_id as sku_id, count(1) from order_line_items
      // left join order_line_item_skus as oli_skus on origin_id = oli_skus.id
      // where order_id = $ and origin_type = 'skuItem' group by sku_id
      val counts = for {
        (skuId, q) ← OrderLineItems.filter(_.orderId === order.id).skuItems
          .join(OrderLineItemSkus).on(_.originId === _.id).groupBy(_._2.skuId)
      } yield (skuId, q.length)

      counts.result.flatMap { (items: Seq[(Int, Int)]) ⇒
        val existingSkuCounts = items.toMap

        val changes = enoughOnHand.map { case (sku, newQuantity) ⇒
          val current = existingSkuCounts.getOrElse(sku.id, 0)

          // we're using absolute values from payload, so if newQuantity is greater then create N items
          if (newQuantity > current) {
            val delta = newQuantity - current

            val queries = for {
              relation ← OrderLineItemSkus.filter(_.skuId === sku.id).one
              origin ← relation match {
                case Some(o)   ⇒ DBIO.successful(o)
                case _         ⇒ OrderLineItemSkus.saveNew(OrderLineItemSku(skuId = sku.id, orderId = order.id))
              }
              bulkInsert ← OrderLineItems ++= (1 to delta).map { _ ⇒ OrderLineItem(0, order.id, origin.id) }.toSeq
            } yield ()

            DbResult.fromDbio(queries)
          } else if (current - newQuantity > 0) {
            // otherwise delete N items
            val queries = for {
              deleteLi ← OrderLineItems.filter(_.id in OrderLineItems.filter(_.orderId === order.id).skuItems
                .join(OrderLineItemSkus).on(_.originId === _.id).filter(_._2.skuId === sku.id).sortBy(_._1.id.asc)
                .take(current - newQuantity).map(_._1.id)).delete

              deleteRel ← newQuantity == 0 match {
                case true   ⇒ OrderLineItemSkus.filter(_.skuId === sku.id).filter(_.orderId === order.id).delete
                case false  ⇒ DBIO.successful({})
              }
            } yield ()

            DbResult.fromDbio(queries)
          } else {
            DbResult.unit
          }
        }.to[Seq]

        DBIO.seq(changes: _*)
      }.flatMap { _ ⇒
        DbResult.fromDbio(OrderLineItems.filter(_.orderId === order.id).result)
      }
    }
  }
}
