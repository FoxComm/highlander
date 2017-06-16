package phoenix.services.orders

import phoenix.models.cord.lineitems._
import phoenix.models.cord.{Cart, Order, OrderShippingMethods, Orders}
import phoenix.models.inventory.Skus
import objectframework.models.{ObjectForms, ObjectShadows}
import core.db.ExPostgresDriver.api._
import objectframework.DbObjectUtils._
import core.utils.Money._
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
    (for {
      lineItem ← OrderLineItems if lineItem.cordRef === cart.refNum
      sku      ← Skus if sku.id === lineItem.skuId
      form     ← ObjectForms if form.id === sku.formId
      shadow   ← ObjectShadows if shadow.id === lineItem.skuShadowId
      illuminated = (form, shadow)
      salePrice   = ((illuminated |→ "salePrice") +>> "value").asColumnOf[Long]
    } yield salePrice).sum.getOrElse(0L).result

  def shippingTotal(order: Order)(implicit ec: EC): DbResultT[Long] =
    for {
      orderShippingMethods ← * <~ OrderShippingMethods.findByOrderRef(order.refNum).result
      sum = orderShippingMethods.map(_.price).sum
    } yield sum

  def adjustmentsTotal(order: Order)(implicit ec: EC): DbResultT[Long] =
    for {
      lineItemAdjustments ← * <~ CartLineItemAdjustments.filter(_.cordRef === order.refNum).result
      sum = lineItemAdjustments.map(_.subtract).sum
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
