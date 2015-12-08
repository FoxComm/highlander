package services.orders

import models.Order
import slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext
import cats.implicits._
import utils.Slick.DbResult
import utils.Slick.implicits._
import utils.DbResultT._
import utils.DbResultT.implicits._

// TODO: Use utils.Money
object OrderTotaler {

  final case class Totals(subTotal: Int, taxes: Int, shipping: Int, adjustments: Int, total: Int)

  object Totals {
    def build(subTotal: Int, shipping: Int, adjustments: Int): Totals = {
      val taxes = ((subTotal - adjustments + shipping) * 0.05).toInt

      Totals(subTotal = subTotal, taxes = taxes, shipping = shipping, adjustments = adjustments,
        total = (adjustments - (subTotal + taxes + shipping)).abs)
    }

    def empty: Totals = Totals(0,0,0,0,0)
  }

  def subTotal(order: Order)(implicit ec: ExecutionContext): DBIO[Int] =
    sql"""select count(*), sum(coalesce(gc.original_balance, 0)) + sum(coalesce(skus.price, 0)) as sum
         |	from order_line_items oli
         |	left outer join order_line_item_skus sli on (sli.id = oli.id)
         |	left outer join skus on (skus.id = sli.sku_id)
         |
         |	left outer join order_line_item_gift_cards gcli on (gcli.id = oli.id)
         |	left outer join gift_cards gc on (gc.id = gcli.gift_card_id)
         |	where oli.order_id = ${order.id}
         | """.stripMargin.as[(Int, Int)].headOption.map {
      case Some((count, total)) if count > 0 ⇒ total
      case _ ⇒ 0
    }

  def shippingTotal(order: Order)(implicit ec: ExecutionContext): DBIO[Int] =
    DBIO.successful(0)

  def adjustmentsTotal(order: Order)(implicit ec: ExecutionContext): DBIO[Int] =
    DBIO.successful(0)

  def totals(order: Order)(implicit ec: ExecutionContext): DBIO[Totals] = for {
    sub   ← subTotal(order)
    ship  ← shippingTotal(order)
    adj   ← adjustmentsTotal(order)
  } yield Totals.build(subTotal = sub, shipping = ship, adjustments = adj)

  def saveTotals(order: Order)(implicit ec: ExecutionContext, db: Database): DbResult[Order] = (for {
    t           ← * <~ totals(order).toXor
    withTotals  = order.copy(subTotal = t.subTotal, shippingTotal = t.shipping,
      adjustmentsTotal = t.adjustments, taxesTotal = t.taxes, grandTotal = t.total)
    updated     ← * <~ models.Orders.update(order, withTotals)
  } yield updated).value
}
