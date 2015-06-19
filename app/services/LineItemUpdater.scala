package services

import models._
import payloads.UpdateLineItemsPayload

import org.scalactic._
import scala.concurrent.{Future, ExecutionContext}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}

import slick.driver.PostgresDriver.api._

object LineItemUpdater {
  val lineItems = TableQuery[OrderLineItems]
  val orders = TableQuery[Orders]

  def updateQuantities(order: Order,
                       payload: Seq[UpdateLineItemsPayload])
                      (implicit ec: ExecutionContext,
                       db: Database): Future[Seq[OrderLineItem] Or List[ErrorMessage]] = {

    // TODO:
    //  validate sku in PIM
    //  execute the fulfillment runner -> creates fulfillments
    //  validate inventory (might be in PIM maybe not)
    //  run hooks to manage promotions

    val updateQuantities = payload.foldLeft(Map[Int, Int]()) { (acc, item) =>
      val quantity = acc.getOrElse(item.skuId, 0)
      acc.updated(item.skuId, quantity + item.quantity)
    }

    // TODO: AW: We should insert some errors/messages into an array for each item that is unavailable.
    // TODO: AW: Add the maximum available to the order if there aren't as many as requested
    Skus.qtyAvailableForGroup(updateQuantities.keys.toSeq).flatMap { availableQuantities =>
      val enoughOnHand = updateQuantities.filter { case (skuId, numRequested) =>
        availableQuantities.get(skuId).exists { numAvailable =>
          numAvailable >= numRequested && numRequested > 0
        }
      }

      // select sku_id, count(1) from line_items where order_id = $ group by sku_id
      val counts = for {
        (skuId, q) <- lineItems.filter(_.orderId === order.id).groupBy(_.skuId)
      } yield (skuId, q.length)

      val queries = counts.result.flatMap { (items: Seq[(Int, Int)]) =>
        val existingSkuCounts = items.toMap

        val changes = enoughOnHand.map { case (skuId, newQuantity) =>
          val current = existingSkuCounts.getOrElse(skuId, 0)
          // we're using absolute values from payload, so if newQuantity is greater then create N items
          if (newQuantity > current) {
            val delta = newQuantity - current

            lineItems ++= (1 to delta).map { _ => OrderLineItem(0, order.id, skuId) }.toSeq
          } else if (current - newQuantity > 0) {
            //otherwise delete N items
            lineItems.filter(_.id in lineItems.filter(_.orderId === order.id).filter(_.skuId === skuId).
              sortBy(_.id.asc).take(current - newQuantity).map(_.id)).delete
          } else {
            // do nothing
            DBIO.successful({})
          }
        }.to[Seq]

        DBIO.seq(changes: _*)
      }.flatMap { _ ⇒
        lineItems.filter(_.orderId === order.id).result
      }

      db.run(queries.transactionally).map(items => Good(items))
    }
  }
}
