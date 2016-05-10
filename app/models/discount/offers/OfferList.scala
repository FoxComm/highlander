package models.discount.offers

import cats.data.Xor
import models.order.Order
import models.order.lineitems._
import models.shipping.ShippingMethod
import cats.data.NonEmptyList
import cats.std.list._
import models.discount.DiscountInput
import models.order.lineitems.OrderLineItemAdjustment._
import services.Result
import utils.aliases._

case class OfferList(offers: Seq[Offer]) extends Offer {

  val adjustmentType: AdjustmentType = ComplexAdjustment

  def adjust(input: DiscountInput)(implicit ec: EC, es: ES): Result[Seq[OrderLineItemAdjustment]] = {
    reject(input, "Not implemented yet")

    /*
    val adjustmentAttempts = offers.map(_.adjust(order, promoId, lineItems, shippingMethod))

    val adjustments: Seq[OrderLineItemAdjustment] = adjustmentAttempts.flatMap {
      o ⇒ o.fold(f ⇒ Seq.empty, qs ⇒ qs.flatMap(q ⇒ Seq(q)))
    }

    val failures = adjustmentAttempts.flatMap {
      o ⇒ o.fold(fs ⇒ fs.unwrap, q ⇒ Seq.empty)
    }

    failures match {
      case head :: tail ⇒ Xor.Left(NonEmptyList(head, tail))
      case Nil          ⇒ Xor.Right(adjustments)
    }
    */
  }
}
