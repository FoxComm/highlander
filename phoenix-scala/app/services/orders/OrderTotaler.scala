package services.orders

import models.cord.lineitems._
import models.cord.{Cart, Order, OrderShippingMethods, Orders}
import models.inventory.ProductVariants
import models.objects.{ObjectForms, ObjectShadows}
import utils.db.ExPostgresDriver.api._
import utils.aliases._
import utils.db._
import utils.db.DbObjectUtils._

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
    (for {
      lineItem ← OrderLineItems if lineItem.cordRef === cart.refNum
      variant  ← ProductVariants if variant.id === lineItem.productVariantId
      form     ← ObjectForms if form.id === variant.formId
      shadow   ← ObjectShadows if shadow.id === lineItem.productVariantShadowId
      illuminated = (form, shadow)
      salePrice   = ((illuminated |→ "salePrice") +>> "value").asColumnOf[Int]
    } yield salePrice).sum.result.map(_.getOrElse(0))

  def shippingTotal(order: Order)(implicit ec: EC): DbResultT[Int] =
    for {
      orderShippingMethods ← * <~ OrderShippingMethods.findByOrderRef(order.refNum).result
      sum = orderShippingMethods.map(_.price).sum
    } yield sum

  def adjustmentsTotal(order: Order)(implicit ec: EC): DbResultT[Int] =
    for {
      lineItemAdjustments ← * <~ OrderLineItemAdjustments.filter(_.cordRef === order.refNum).result
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
