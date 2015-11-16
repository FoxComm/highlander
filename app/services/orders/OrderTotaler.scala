package services.orders

import models.Order
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext
import cats.implicits._
import utils.Slick.implicits._

object OrderTotaler {
  def subTotal(order: Order)(implicit ec: ExecutionContext): DBIO[Option[Int]] =
    sql"""select count(*), sum(coalesce(gc.original_balance, 0)) + sum(coalesce(skus.price, 0)) as sum
         |	from order_line_items oli
         |	left outer join order_line_item_skus sli on (sli.id = oli.id)
         |	left outer join skus on (skus.id = sli.sku_id)
         |
         |	left outer join order_line_item_gift_cards gcli on (gcli.id = oli.id)
         |	left outer join gift_cards gc on (gc.id = gcli.gift_card_id)
         |	where oli.order_id = ${order.id}
         | """.stripMargin.as[(Int, Int)].headOption.map {
      case Some((count, total)) if count > 0 ⇒ total.some
      case _ ⇒ None
    }

  def grandTotal(order: Order)(implicit ec: ExecutionContext): DBIO[Option[Int]] =
    subTotal(order)
}
