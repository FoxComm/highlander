package phoenix.responses.cord.base

import phoenix.models.cord.{Cart, Order}
import phoenix.responses.ResponseItem

case class OrderResponseTotals(subTotal: Long, taxes: Long, shipping: Long, adjustments: Long, total: Long)
    extends ResponseItem

object OrderResponseTotals {

  def build(order: Order): OrderResponseTotals =
    OrderResponseTotals(subTotal = order.subTotal,
                        shipping = order.shippingTotal,
                        adjustments = order.adjustmentsTotal,
                        taxes = order.taxesTotal,
                        total = order.grandTotal)

}

case class CartResponseTotals(subTotal: Long,
                              taxes: Long,
                              shipping: Long,
                              adjustments: Long,
                              total: Long,
                              customersExpenses: Long)
    extends ResponseItem

object CartResponseTotals {

  def empty: CartResponseTotals = CartResponseTotals(0, 0, 0, 0, 0, 0)

  def build(cart: Cart, coveredByInStoreMethods: Long): CartResponseTotals =
    CartResponseTotals(
      subTotal = cart.subTotal,
      shipping = cart.shippingTotal,
      adjustments = cart.adjustmentsTotal,
      taxes = cart.taxesTotal,
      total = cart.grandTotal,
      customersExpenses = cart.grandTotal - coveredByInStoreMethods
    )

}
