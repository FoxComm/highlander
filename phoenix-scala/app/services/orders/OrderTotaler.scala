package services.orders

import models.cord.lineitems._
import models.cord.{Order, OrderShippingMethods, Orders, Cart}
import slick.driver.PostgresDriver.api._
import utils.aliases._
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

  def subTotal(cart: Cart, order: Order)(implicit ec: EC): DBIO[Int] =
    sql"""select count(*), sum(coalesce(cast(vform.attributes->(vshadow.attributes->'salePrice'->>'ref')->>'value' as
       |  integer), 0)) as sum
       |	from order_line_items oli
       |	left outer join product_variants variant on (variant.id = oli.product_variant_id)
       |	left outer join object_forms vform on (vform.id = variant.form_id)
       |	left outer join object_shadows vshadow on (vshadow.id = oli.variant_shadow_id)
       |
       |	where oli.cord_ref = ${cart.refNum}
       | """.stripMargin.as[(Int, Int)].headOption.map {
      case Some((count, total)) if count > 0 ⇒ total
      case _                                 ⇒ 0
    }

  def shippingTotal(order: Order)(implicit ec: EC): DbResultT[Int] =
    for {
      orderShippingMethods ← * <~ OrderShippingMethods.findByOrderRef(order.refNum).result
      sum = orderShippingMethods.foldLeft(0)(_ + _.price)
    } yield sum

  def adjustmentsTotal(order: Order)(implicit ec: EC): DbResultT[Int] =
    for {
      lineItemAdjustments ← * <~ OrderLineItemAdjustments.filter(_.cordRef === order.refNum).result
      sum = lineItemAdjustments.foldLeft(0)(_ + _.subtract)
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
