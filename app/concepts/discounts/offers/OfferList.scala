package concepts.discounts.offers

import Offer._
import cats.data.Xor
import models.order.Order
import models.order.lineitems._
import models.shipping.ShippingMethod
import cats.data.NonEmptyList
import cats.std.list._

case class OfferList(offers: Seq[Offer]) extends Offer {

  def adjust(order: Order, promoId: Int, lineItems: Seq[OrderLineItemProductData],
    shippingMethod: Option[ShippingMethod]): AdjustmentResult = {

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
  }
}
