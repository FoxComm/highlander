package services

import models.{Carts, Cart, CartLineItems, CartLineItem, Order}
import payloads.UpdateLineItemsPayload

import org.scalactic._
import scala.concurrent.{Future, ExecutionContext}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}

import slick.driver.PostgresDriver.api._

object LineItemUpdater {
  val lineItems = TableQuery[CartLineItems]
  val carts = TableQuery[Carts]

  def updateQuantities(cart: Cart,
                       payload: Seq[UpdateLineItemsPayload])
                      (implicit ec: ExecutionContext,
                       db: Database): Future[Seq[CartLineItem] Or List[ErrorMessage]] = {

    // TODO:
    //  validate sku in PIM
    //  execute the fulfillment runner -> creates fulfillments
    //  validate inventory (might be in PIM maybe not)
    //  run hooks to manage promotions


    val updateQuantities = payload.foldLeft(Map[Int, Int]()) { (acc, item) =>
      val quantity = acc.getOrElse(item.skuId, 0)
      acc.updated(item.skuId, quantity + item.quantity)
    }

    // select sku_id, count(1) from line_items where cart_id = $ group by sku_id
    val counts = for {
      (skuId, q) <- lineItems.filter(_.cartId === cart.id).groupBy(_.skuId)
    } yield (skuId, q.length)

    val queries = counts.result.flatMap { (items: Seq[(Int, Int)]) =>
      val existingSkuCounts = items.toMap

      val changes = updateQuantities.map { case (skuId, newQuantity) =>
        val current = existingSkuCounts.getOrElse(skuId, 0)
        // we're using absolute values from payload, so if newQuantity is greater then create N items
        if (newQuantity > current) {
          val delta = newQuantity - current

          lineItems ++= (1 to delta).map { _ => CartLineItem(0, cart.id, skuId) }.toSeq
        } else if (current - newQuantity > 0) { //otherwise delete N items
          lineItems.filter(_.id in lineItems.filter(_.cartId === cart.id).filter(_.skuId === skuId).
            sortBy(_.id.asc).take(current - newQuantity).map(_.id)).delete
        } else {
          // do nothing
          DBIO.successful({})
        }
      }.to[Seq]

      DBIO.seq(changes: _*)
    }.flatMap { _ ⇒
      lineItems.filter(_.cartId === cart.id).result
    }

    db.run(queries.transactionally).map(items => Good(items))
  }
}
