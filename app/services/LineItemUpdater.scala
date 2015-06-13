package services

import models.{Carts, Cart, LineItems, LineItem, Order}
import payloads.UpdateLineItemsPayload

import org.scalactic._
import scala.concurrent.{Future, ExecutionContext}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}

import slick.driver.PostgresDriver.api._

sealed class LineItemParent[T]
object LineItemParent{
  implicit object CartParent extends LineItemParent[Cart]
  implicit object OrderParent extends LineItemParent[Order]
}

object LineItemUpdater {
  val lineItems = TableQuery[LineItems]
  val carts = TableQuery[Carts]

  def updateQuantities[P: LineItemParent](parent: P,
                       payload: Seq[UpdateLineItemsPayload])
                      (implicit ec: ExecutionContext,
                       db: Database): Future[Seq[LineItem] Or List[ErrorMessage]] = {

    // TODO:
    //  validate sku in PIM
    //  execute the fulfillment runner -> creates fulfillments
    //  validate inventory (might be in PIM maybe not)
    //  run hooks to manage promotions

    // Quick method to make the method accept polymorphic parents.
    val parentInfo: (String, Int) = parent match {
      case c: Cart => ("cart", c.id)
      case o: Order => ("order", o.id)
      case _ => throw new UnsupportedOperationException(s"not implemented for type ${parent.getClass}")
    }

    val updateQuantities = payload.foldLeft(Map[Int, Int]()) { (acc, item) =>
      val quantity = acc.getOrElse(item.skuId, 0)
      acc.updated(item.skuId, quantity + item.quantity)
    }

    // select sku_id, count(1) from line_items where cart_id = $ group by sku_id
    val counts = for {
      (skuId, q) <- lineItems.filter(_.parentId === parentInfo._2).filter(_.parentType === parentInfo._1).groupBy(_.skuId)
    } yield (skuId, q.length)

    val queries = counts.result.flatMap { (items: Seq[(Int, Int)]) =>
      val existingSkuCounts = items.toMap

      val changes = updateQuantities.map { case (skuId, newQuantity) =>
        val current = existingSkuCounts.getOrElse(skuId, 0)
        // we're using absolute values from payload, so if newQuantity is greater then create N items
        if (newQuantity > current) {
          val delta = newQuantity - current

          lineItems ++= (1 to delta).map { _ => LineItem(0, parentInfo._2, parentInfo._1, skuId) }.toSeq
        } else if (current - newQuantity > 0) { //otherwise delete N items
          lineItems.filter(_.id in lineItems.filter(_.parentId === parentInfo._2).filter(_.parentType === parentInfo._1).filter(_.skuId === skuId).
            sortBy(_.id.asc).take(current - newQuantity).map(_.id)).delete
        } else {
          // do nothing
          DBIO.successful({})
        }
      }.to[Seq]

      DBIO.seq(changes: _*)
    }.flatMap { _ ⇒
      lineItems.filter(_.parentId === parentInfo._2).filter(_.parentType === parentInfo._1).result
    }

    db.run(queries.transactionally).map(items => Good(items))
  }

  def deleteById(id: Int, cartId: Int)
                (implicit ec: ExecutionContext,
                 db: Database): Future[Seq[LineItem] Or One[ErrorMessage]] = {

    val actions = for {
      numDeleted <- lineItems.filter(_.id === id).delete
      lineItems <- lineItems.filter(_.parentId === cartId).filter(_.parentType === "cart").result
    } yield (numDeleted, lineItems)

    db.run(actions.transactionally).map { case (numDeleted, lineItems) =>
      if (numDeleted == 0) {
        Bad(One(s"could not find lineItem with id=$id"))
      } else {
        Good(lineItems)
      }
    }
  }
}
