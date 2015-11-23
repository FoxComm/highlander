package services.rmas

import cats.syntax.order
import models.Rma
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext
import cats.implicits._
import utils.Slick.implicits._

object RmaTotaler {
  def subTotal(rma: Rma)(implicit ec: ExecutionContext): DBIO[Option[Int]] =
    sql"""select count(*), sum(coalesce(gc.original_balance, 0)) + sum(coalesce(skus.price, 0)) as sum
       |	from rma_line_items rli
       |	left outer join rma_line_item_skus sli on (sli.id = rli.id)
       |	left outer join skus on (skus.id = sli.sku_id)
       |
       |	left outer join rma_line_item_gift_cards gcli on (gcli.id = rli.id)
       |	left outer join gift_cards gc on (gc.id = gcli.gift_card_id)
       |	where rli.rma_id = ${rma.id}
       | """.stripMargin.as[(Int, Int)].headOption.map {
      case Some((count, total)) if count > 0 ⇒ total.some
      case _ ⇒ None
    }
}
