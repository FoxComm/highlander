package services

import scala.concurrent.ExecutionContext

import cats.data.Xor
import cats.data.Validated.{Valid, Invalid}
import models._
import models.OrderLineItems.scope._
import payloads.{AddGiftCardLineItem, UpdateLineItemsPayload}
import cats.implicits._
import responses.FullOrder
import slick.driver.PostgresDriver.api._
import utils.Slick._
import utils.Slick.implicits._

object LineItemUpdater {
  val lineItems = TableQuery[OrderLineItems]
  val orders = TableQuery[Orders]

  def addGiftCard(refNum: String, payload: AddGiftCardLineItem)
    (implicit ec: ExecutionContext, db: Database): Result[FullOrder.Root] = {

    payload.validate match {
      case Valid(_)        ⇒ addGiftCardToOrder(refNum, payload).run()
      case Invalid(errors) ⇒ Result.failures(errors)
    }
  }

  private def addGiftCardToOrder(refNum: String, payload: AddGiftCardLineItem)
  (implicit ec: ExecutionContext, db: Database) = {
    Orders.findCartByRefNum(refNum).one.flatMap {
      case Some(order) ⇒
        val queries = for {
          gcOrigin ← GiftCardOrders.save(GiftCardOrder(orderId = order.id))
          gc ← GiftCards.save(GiftCard.buildLineItem(balance = payload.balance, originId = gcOrigin.id,
            currency = payload.currency))
          lineItemGc ← OrderLineItemGiftCards.save(OrderLineItemGiftCard(giftCardId = gc.id, orderId = order.id))
          lineItem ← OrderLineItems.save(OrderLineItem.buildGiftCard(order, lineItemGc))
        } yield ()

        DbResult.fromDbio(queries.transactionally >> FullOrder.fromOrder(order))
      case None ⇒
        DbResult.failure(OrderNotFoundFailure(refNum))
    }
  }

  def editGiftCard(refNum: String, code: String, payload: AddGiftCardLineItem)
    (implicit ec: ExecutionContext, db: Database): Result[FullOrder.Root] = {

    payload.validate match {
      case Valid(_) ⇒
        GiftCards.findCartByCode(code).selectOneForUpdate { gc ⇒
          val updatedGc = gc.copy(originalBalance = payload.balance,
            availableBalance = payload.balance, currentBalance = payload.balance, currency = payload.currency)

          val update = GiftCards.filter(_.id === gc.id).update(updatedGc)

          Orders.findCartByRefNum(refNum).one.flatMap {
            case Some(o) ⇒ DbResult.fromDbio(update >> FullOrder.fromOrder(o))
            case None    ⇒ DbResult.failure(OrderNotFoundFailure(refNum))
          }
        }
      case Invalid(errors) ⇒
        Result.failures(errors)
    }
  }

  def deleteGiftCard(refNum: String, code: String)
    (implicit ec: ExecutionContext, db: Database): Result[FullOrder.Root] = {

    GiftCards.findCartByCode(code).selectOneForUpdate { gc ⇒
      val origin = OrderLineItemGiftCards.filter(_.giftCardId === gc.id)
      origin.one.flatMap {
        case Some(o) ⇒
          val deleteAll = for {
            lineItemGiftCard ← origin.delete
            lineItem ← OrderLineItems.filter(_.originId === o.id).giftCards.delete
            giftCard ← GiftCards.filter(_.id === gc.id).delete
            gcOrigin ← GiftCardOrders.filter(_.id === gc.originId).delete
          } yield ()

          Orders.findCartByRefNum(refNum).one.flatMap {
            case Some(order)  ⇒ DbResult.fromDbio(deleteAll >> FullOrder.fromOrder(order))
            case None         ⇒ DbResult.failure(OrderNotFoundFailure(refNum))
          }
        case None ⇒
          DbResult.failure(GiftCardNotFoundFailure(code))
      }
    }
  }

  @SuppressWarnings(Array("org.brianmckenna.wartremover.warts.Any"))
  def updateQuantities(order: Order, payload: Seq[UpdateLineItemsPayload])
                      (implicit ec: ExecutionContext, db: Database): Result[FullOrder.Root] = {

    // TODO:
    //  validate sku in PIM
    //  execute the fulfillment runner -> creates fulfillments
    //  validate inventory (might be in PIM maybe not)
    //  run hooks to manage promotions

    (for {
      _         ← ResultT(update(order, payload))
      response ← ResultT.right(FullOrder.fromOrder(order).run())
    } yield response).value
  }

  private def update(order: Order, payload: Seq[UpdateLineItemsPayload])
    (implicit ec: ExecutionContext, db: Database): Result[Seq[OrderLineItem]] = {

    val updateQuantities = payload.foldLeft(Map[String, Int]()) { (acc, item) =>
      val quantity = acc.getOrElse(item.sku, 0)
      acc.updated(item.sku, quantity + item.quantity)
    }

    // TODO: AW: We should insert some errors/messages into an array for each item that is unavailable.
    // TODO: AW: Add the maximum available to the order if there aren't as many as requested
    Skus.qtyAvailableForSkus(updateQuantities.keys.toSeq).flatMap { availableQuantities =>
      val enoughOnHand = availableQuantities.foldLeft(Map.empty[Sku, Int]) { case (acc, (sku, numAvailable)) =>
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
        (skuId, q) <- lineItems.filter(_.orderId === order.id).skuItems
          .join(OrderLineItemSkus).on(_.originId === _.id).groupBy(_._2.skuId)
      } yield (skuId, q.length)

      val queries = counts.result.flatMap { (items: Seq[(Int, Int)]) =>
        val existingSkuCounts = items.toMap

        val changes = enoughOnHand.map { case (sku, newQuantity) =>
          val current = existingSkuCounts.getOrElse(sku.id, 0)

          // we're using absolute values from payload, so if newQuantity is greater then create N items
          if (newQuantity > current) {
            val delta = newQuantity - current

            val queries = for {
              relation <- OrderLineItemSkus.filter(_.skuId === sku.id).one
              origin ← relation match {
                case Some(o)   ⇒ DBIO.successful(o)
                case _         ⇒ OrderLineItemSkus.save(OrderLineItemSku(skuId = sku.id, orderId = order.id))
              }
              bulkInsert ← lineItems ++= (1 to delta).map { _ => OrderLineItem(0, order.id, origin.id) }.toSeq
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
            // do nothing
            DBIO.successful({})
          }
        }.to[Seq]

        DBIO.seq(changes: _*)
      }.flatMap { _ ⇒
        lineItems.filter(_.orderId === order.id).result
      }

      Result.fromFuture(db.run(queries.transactionally))
    }
  }
}
