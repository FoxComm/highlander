package models.discount.offers

import cats.data.Xor
import cats.data.NonEmptyList
import cats.std.list._
import models.discount.DiscountInput
import models.discount.offers.Offer.OfferResult
import models.order.lineitems.OrderLineItemAdjustment._

case class OfferList(offers: Seq[Offer]) extends Offer {

  val adjustmentType: AdjustmentType = Combinator

  def adjust(input: DiscountInput): OfferResult = {
    val adjustmentAttempts = offers.map(_.adjust(input))

    val adjustments = adjustmentAttempts.flatMap {
      o ⇒ o.fold(f ⇒ Seq.empty, qs ⇒ qs.flatMap(q ⇒ Seq(q)))
    }

    val failures = adjustmentAttempts.flatMap(o ⇒ o.fold(fs ⇒ fs.unwrap, q ⇒ Seq.empty))

    failures match {
      case head :: tail ⇒ Xor.Left(NonEmptyList(head, tail))
      case Nil          ⇒ Xor.Right(adjustments)
    }
  }
}
