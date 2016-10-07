package responses.cord.base

import models.cord.CordBase
import responses.ResponseItem

case class CordResponseTotals(subTotal: Int,
                              taxes: Int,
                              shipping: Int,
                              adjustments: Int,
                              total: Int)
    extends ResponseItem

object CordResponseTotals {

  def empty: CordResponseTotals = CordResponseTotals(0, 0, 0, 0, 0)

  def build[C <: CordBase[C]](cord: C): CordResponseTotals =
    CordResponseTotals(subTotal = cord.subTotal,
                       shipping = cord.shippingTotal,
                       adjustments = cord.adjustmentsTotal,
                       taxes = cord.taxesTotal,
                       total = cord.grandTotal)

}
