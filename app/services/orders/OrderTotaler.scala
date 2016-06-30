package services.orders

import models.order.lineitems._
import models.order.{Order, OrderShippingMethods, Orders}
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db.DbResultT._
import utils.db._

// TODO: Use utils.Money
object OrderTotaler {

  case class Totals(subTotal: Int, taxes: Int, shipping: Int, adjustments: Int, total: Int)

  object Totals {
    def build(subTotal: Int, shipping: Int, adjustments: Int): Totals = {
      val taxes = ((subTotal - adjustments + shipping) * 0.05).toInt

      Totals(subTotal = subTotal,
             taxes = taxes,
             shipping = shipping,
             adjustments = adjustments,
             total = (adjustments - (subTotal + taxes + shipping)).abs)
    }

    def empty: Totals = Totals(0, 0, 0, 0, 0)
  }

  def subTotal(order: Order)(implicit ec: EC): DBIO[Int] =
    sql"""select count(*), sum(coalesce(gc.original_balance, 0)) + sum(coalesce(cast(sku_form.attributes->(sku_shadow.attributes->'salePrice'->>'ref')->>'value' as integer), 0)) as sum
         |	from order_line_items oli
         |	left outer join order_line_item_skus sli on (sli.id = oli.origin_id)
         |	left outer join skus sku on (sku.id = sli.sku_id)
         |	left outer join object_forms sku_form on (sku_form.id = sku.form_id)
         |	left outer join object_shadows sku_shadow on (sku_shadow.id = sli.sku_shadow_id)
         |
         |	left outer join order_line_item_gift_cards gcli on (gcli.id = oli.origin_id)
         |	left outer join gift_cards gc on (gc.id = gcli.gift_card_id)
         |	where oli.order_ref = ${order.refNum}
         | """.stripMargin.as[(Int, Int)].headOption.map {
      case Some((count, total)) if count > 0 ⇒ total
      case _                                 ⇒ 0
    }

  def shippingTotal(order: Order)(implicit ec: EC): DbResult[Int] =
    (for {
      orderShippingMethods ← * <~ OrderShippingMethods.findByOrderRef(order.refNum).result.toXor
      sum = orderShippingMethods.foldLeft(0)(_ + _.price)
    } yield sum).value

  def adjustmentsTotal(order: Order)(implicit ec: EC): DbResult[Int] =
    (for {
      lineItemAdjustments ← * <~ OrderLineItemAdjustments
                             .filter(_.orderRef === order.refNum)
                             .result
                             .toXor
      sum = lineItemAdjustments.foldLeft(0)(_ + _.substract)
    } yield sum).value

  def totals(order: Order)(implicit ec: EC): DbResult[Totals] =
    (for {
      sub  ← * <~ subTotal(order).toXor
      ship ← * <~ shippingTotal(order)
      adj  ← * <~ adjustmentsTotal(order)
    } yield Totals.build(subTotal = sub, shipping = ship, adjustments = adj)).value

  def saveTotals(order: Order)(implicit ec: EC): DbResult[Order] =
    (for {
      t ← * <~ totals(order)
      withTotals = order.copy(subTotal = t.subTotal,
                              shippingTotal = t.shipping,
                              adjustmentsTotal = t.adjustments,
                              taxesTotal = t.taxes,
                              grandTotal = t.total)
      updated ← * <~ Orders.update(order, withTotals)
    } yield updated).value
}
