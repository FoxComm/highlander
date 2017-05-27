package phoenix.services.orders

import phoenix.models.cord.lineitems._
import phoenix.models.cord.{Cart, Order, OrderShippingMethods, Orders}
import slick.jdbc.PostgresProfile.api._
import utils.Money._
import phoenix.utils.aliases._
import core.db._

// TODO: Use utils.Money
object OrderTotaler {

  case class Totals(subTotal: Long, taxes: Long, shipping: Long, adjustments: Long, total: Long)

  object Totals {
    def build(subTotal: Long, shipping: Long, adjustments: Long): Totals = {
      val taxes: Long = (subTotal - adjustments + shipping).applyTaxes(0.05)

      Totals(subTotal = subTotal,
             taxes = taxes,
             shipping = shipping,
             adjustments = adjustments,
             total = (adjustments - (subTotal + taxes + shipping)).abs)
    }

    def empty: Totals = Totals(0, 0, 0, 0, 0)
  }

  def subTotal(cart: Cart, order: Order)(implicit ec: EC): DBIO[Long] =
    sql"""select count(*), sum(coalesce(cast(sku_form.attributes->(sku_shadow.attributes->'salePrice'->>'ref')->>'value' as integer), 0)) as sum
          |	from order_line_items oli
          |	left outer join skus sku on (sku.id = oli.sku_id)
          |	left outer join object_forms sku_form on (sku_form.id = sku.form_id)
          |	left outer join object_shadows sku_shadow on (sku_shadow.id = oli.sku_shadow_id)
          |
          |	where oli.cord_ref = ${cart.refNum}
          | """.stripMargin.as[(Int, Long)].headOption.map {
      case Some((count, total)) if count > 0 ⇒ total
      case _                                 ⇒ 0
    }

  def shippingTotal(order: Order)(implicit ec: EC): DbResultT[Long] =
    for {
      orderShippingMethods ← * <~ OrderShippingMethods.findByOrderRef(order.refNum).result
      sum = orderShippingMethods.foldLeft(0L)(_ + _.price)
    } yield sum

  def adjustmentsTotal(order: Order)(implicit ec: EC): DbResultT[Long] =
    for {
      lineItemAdjustments ← * <~ CartLineItemAdjustments.filter(_.cordRef === order.refNum).result
      sum = lineItemAdjustments.foldLeft(0L)(_ + _.subtract)
    } yield sum

  def totals(cart: Cart, order: Order)(implicit ec: EC): DbResultT[Totals] =
    for {
      sub  ← * <~ subTotal(cart, order)
      ship ← * <~ shippingTotal(order)
      adj  ← * <~ adjustmentsTotal(order)
    } yield Totals.build(subTotal = sub, shipping = ship, adjustments = adj)

  def saveTotals(cart: Cart, order: Order)(implicit ec: EC): DbResultT[Order] =
    for {
      t ← * <~ totals(cart, order)
      withTotals = order.copy(subTotal = t.subTotal,
                              shippingTotal = t.shipping,
                              adjustmentsTotal = t.adjustments,
                              taxesTotal = t.taxes,
                              grandTotal = t.total)
      updated ← * <~ Orders.update(order, withTotals)
    } yield updated
}
