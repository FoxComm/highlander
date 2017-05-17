package responses.cord.base

import models.cord.{Cart, Order}
import responses.ResponseItem

case class OrderResponseTotals(subTotal: Int,
                               taxes: Int,
                               shipping: Int,
                               adjustments: Int,
                               total: Int)
    extends ResponseItem

object OrderResponseTotals {

  def build(order: Order): OrderResponseTotals =
    OrderResponseTotals(subTotal = order.subTotal,
                        shipping = order.shippingTotal,
                        adjustments = order.adjustmentsTotal,
                        taxes = order.taxesTotal,
                        total = order.grandTotal)

}

case class CartResponseTotals(subTotal: Int,
                              taxes: Int,
                              shipping: Int,
                              adjustments: Int,
                              total: Int,
                              customersExpenses: Int)
    extends ResponseItem

object CartResponseTotals {

  def empty: CartResponseTotals = CartResponseTotals(0, 0, 0, 0, 0, 0)

  def build(cart: Cart, coveredByInStoreMethods: Int): CartResponseTotals =
    CartResponseTotals(subTotal = cart.subTotal,
                       shipping = cart.shippingTotal,
                       adjustments = cart.adjustmentsTotal,
                       taxes = cart.taxesTotal,
                       total = cart.grandTotal,
                       customersExpenses = cart.grandTotal - coveredByInStoreMethods)

}
