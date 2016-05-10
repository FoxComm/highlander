package models.discount.offers

import cats.implicits._
import cats.data.Xor
import failures._
import models.order.lineitems.OrderLineItemAdjustment

case class OfferResult(adjustments: Seq[OrderLineItemAdjustment] = Seq.empty, warnings: Option[Failures] = None)

object OfferResult {

  def fromXor(xor: Xor[Failures, Seq[OrderLineItemAdjustment]]): OfferResult = xor match {
    case Xor.Left(failures) ⇒ OfferResult(warnings = failures.some)
    case Xor.Right(adjs)    ⇒ OfferResult(adjustments = adjs)
  }
}