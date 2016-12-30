package responses.cord.base

import models.cord.CordBase
import responses.ResponseItem

sealed trait CordResponseTotals {
  def subTotal: Int
  def taxes: Int
  def shipping: Int
  def adjustments: Int
  def total: Int
}

case class OrderResponseTotals(subTotal: Int,
                               taxes: Int,
                               shipping: Int,
                               adjustments: Int,
                               total: Int)
    extends CordResponseTotals
    with ResponseItem

object OrderResponseTotals {

  def empty: OrderResponseTotals = OrderResponseTotals(0, 0, 0, 0, 0)

  def build[C <: CordBase[C]](cord: C): OrderResponseTotals =
    OrderResponseTotals(subTotal = cord.subTotal,
                        shipping = cord.shippingTotal,
                        adjustments = cord.adjustmentsTotal,
                        taxes = cord.taxesTotal,
                        total = cord.grandTotal)

}

case class CartResponseTotals(subTotal: Int,
                              taxes: Int,
                              shipping: Int,
                              adjustments: Int,
                              total: Int,
                              creditCardCharge: Int)
    extends CordResponseTotals
    with ResponseItem

object CartResponseTotals {

  def empty: CartResponseTotals = CartResponseTotals(0, 0, 0, 0, 0, 0)

  def build[C <: CordBase[C]](cord: C, decreaseChargeBy: Int): CartResponseTotals =
    CartResponseTotals(subTotal = cord.subTotal,
                       shipping = cord.shippingTotal,
                       adjustments = cord.adjustmentsTotal,
                       taxes = cord.taxesTotal,
                       total = cord.grandTotal,
                       creditCardCharge = cord.grandTotal - decreaseChargeBy)

}
