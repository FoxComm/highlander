package models.discount.offers

import cats.implicits._
import cats.data.Xor
import failures._
import models.order.lineitems.OrderLineItemAdjustment

case class OfferContainer(
    adjustments: Seq[OrderLineItemAdjustment] = Seq.empty, warnings: Option[Failures] = None)

object OfferContainer {

  def fromXor(xor: Xor[Failures, Seq[OrderLineItemAdjustment]]): OfferContainer = xor match {
    case Xor.Left(failures) ⇒ OfferContainer(warnings = failures.some)
    case Xor.Right(adjs)    ⇒ OfferContainer(adjustments = adjs)
  }
}
