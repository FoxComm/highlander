package services

import scala.concurrent.ExecutionContext

import cats.data.Xor
import cats.data.Validated.{Valid, Invalid}
import models._
import CartFailures.OrderMustBeCart
import models.OrderLineItems.scope._
import payloads.{AddGiftCardLineItem, UpdateLineItemsPayload}
import cats.implicits._
import responses.FullOrder
import services.CartFailures.OrderMustBeCart
import slick.driver.PostgresDriver.api._
import utils.Slick._
import utils.Slick.implicits._

object LineItemUpdater {
  val lineItems = TableQuery[OrderLineItems]
  val orders = TableQuery[Orders]

  def addGiftCard(refNum: String, payload: AddGiftCardLineItem)
    (implicit ec: ExecutionContext, db: Database): Result[FullOrder.Root] = {

    payload.validate match {
      case Valid(_)        ⇒ addGiftCardToOrder(refNum, payload)
      case Invalid(errors) ⇒ Result.failures(errors)
    }
  }

  private def addGiftCardToOrder(refNum: String, payload: AddGiftCardLineItem)
    (implicit ec: ExecutionContext, db: Database) = {
    val finder = Orders.findByRefNum(refNum)

    finder.selectOne ({ order ⇒
      val queries = for {
        gcOrigin ← GiftCardOrders.saveNew(GiftCardOrder(orderId = order.id))
        gc ← GiftCards.saveNew(GiftCard.buildLineItem(balance = payload.balance, originId = gcOrigin.id,
          currency = payload.currency))
        lineItemGc ← OrderLineItemGiftCards.saveNew(OrderLineItemGiftCard(giftCardId = gc.id, orderId = order.id))
        lineItem ← OrderLineItems.saveNew(OrderLineItem.buildGiftCard(order, lineItemGc))
      } yield ()

      DbResult.fromDbio(queries >> fullOrder(finder))
    }, checks = finder.checks + finder.mustBeCart)
  }

  def editGiftCard(refNum: String, code: String, payload: AddGiftCardLineItem)
    (implicit ec: ExecutionContext, db: Database): Result[FullOrder.Root] = {

    payload.validate match {
      case Valid(_) ⇒
        val finder = GiftCards.findByCode(code)
        finder.selectOneForUpdate ({ gc ⇒
          val updatedGc = gc.copy(originalBalance = payload.balance,
            availableBalance = payload.balance, currentBalance = payload.balance, currency = payload.currency)

          val update = GiftCards.filter(_.id === gc.id).update(updatedGc)

          Orders.findByRefNum(refNum).one.flatMap {
            case Some(order) if order.isCart ⇒ DbResult.fromDbio(update >> FullOrder.fromOrder(order))
            case Some(order)                 ⇒ DbResult.failure(OrderMustBeCart(order.refNum))
            case None                        ⇒ DbResult.failure(NotFoundFailure404(Order, refNum))
          }
        }, checks = finder.checks + finder.mustBeCart)
      case Invalid(errors) ⇒
        Result.failures(errors)
    }
  }

  def deleteGiftCard(refNum: String, code: String)
    (implicit ec: ExecutionContext, db: Database): Result[FullOrder.Root] = {

    val finder = GiftCards.findByCode(code)
    finder.selectOneForUpdate ({ gc ⇒
      val origin = OrderLineItemGiftCards.filter(_.giftCardId === gc.id)
      origin.one.flatMap {
        case Some(o) ⇒
          val deleteAll = for {
            lineItemGiftCard ← origin.delete
            lineItem ← OrderLineItems.filter(_.originId === o.id).giftCards.delete
            giftCard ← GiftCards.filter(_.id === gc.id).delete
            gcOrigin ← GiftCardOrders.filter(_.id === gc.originId).delete
          } yield ()

          Orders.findByRefNum(refNum).one.flatMap {
            case Some(order) if order.isCart ⇒ DbResult.fromDbio(deleteAll >> FullOrder.fromOrder(order))
            case Some(order)                 ⇒ DbResult.failure(OrderMustBeCart(order.refNum))
            case None                        ⇒ DbResult.failure(NotFoundFailure404(Order, refNum))
          }
        case None ⇒
          DbResult.failure(NotFoundFailure404(GiftCard, code))
      }
    }, checks = finder.checks + finder.mustBeCart)
  }

  def updateQuantitiesOnOrder(refNum: String, payload: Seq[UpdateLineItemsPayload])
    (implicit ec: ExecutionContext, db: Database): Result[FullOrder.Root] = {
    val finder = Orders.findByRefNum(refNum)

    finder.selectOneForUpdate { order ⇒
      DbResult.fromDbio(updateQuantities(order, payload) >> fullOrder(finder))
    }
  }

  def updateQuantitiesOnCustomersOrder(customer: Customer, payload: Seq[UpdateLineItemsPayload])
    (implicit ec: ExecutionContext, db: Database): Result[FullOrder.Root] = {
    val finder = Orders.findActiveOrderByCustomer(customer)

    finder.selectOneForUpdate { order ⇒
      DbResult.fromDbio(updateQuantities(order, payload) >> fullOrder(finder))
    }
  }

  // TODO:
  //  validate sku in PIM
  //  execute the fulfillment runner → creates fulfillments
  //  validate inventory (might be in PIM maybe not)
  //  run hooks to manage promotions
  private def updateQuantities(order: Order, payload: Seq[UpdateLineItemsPayload])
    (implicit ec: ExecutionContext, db: Database): DbResult[Seq[OrderLineItem]] = {

    val updateQuantities = payload.foldLeft(Map[String, Int]()) { (acc, item) ⇒
      val quantity = acc.getOrElse(item.sku, 0)
      acc.updated(item.sku, quantity + item.quantity)
    }

    // TODO: AW: We should insert some errors/messages into an array for each item that is unavailable.
    // TODO: AW: Add the maximum available to the order if there aren't as many as requested
    Skus.qtyAvailableForSkus(updateQuantities.keys.toSeq).flatMap { availableQuantities ⇒
      val enoughOnHand = availableQuantities.foldLeft(Map.empty[Sku, Int]) { case (acc, (sku, numAvailable)) ⇒
        val numRequested = updateQuantities.getOrElse(sku.sku, 0)
        if (numAvailable >= numRequested && numRequested >= 0)
          acc.updated(sku, numRequested)
        else
          acc
      }

      // select oli_skus.sku_id as sku_id, count(1) from order_line_items
      // left join order_line_item_skus as oli_skus on origin_id = oli_skus.id
      // where order_id = $ and origin_type = 'skuItem' group by sku_id
      val counts = for {
        (skuId, q) ← lineItems.filter(_.orderId === order.id).skuItems
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
              bulkInsert ← lineItems ++= (1 to delta).map { _ ⇒ OrderLineItem(0, order.id, origin.id) }.toSeq
            } yield ()

            DbResult.fromDbio(queries)
          } else if (current - newQuantity > 0) {
            // otherwise delete N items
            val queries = for {
              deleteLi ← lineItems.filter(_.id in lineItems.filter(_.orderId === order.id).skuItems
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
        DbResult.fromDbio(lineItems.filter(_.orderId === order.id).result)
      }
    }
  }
}
