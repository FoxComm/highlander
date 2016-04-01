package concepts.discounts.offers

import cats.data.Xor
import Offer.AdjustmentResult
import concepts.discounts._
import failures.DiscountCompilerFailures._
import models.order.Order
import models.order.lineitems.OrderLineItemProductData
import models.shipping.ShippingMethod

final case class ItemsSelectPercentOffer(discount: Int, references: Seq[ReferenceTuple]) extends Offer {

  val rejectionReason = "Not implemented yet"

  def adjust(order: Order, lineItems: Seq[OrderLineItemProductData],
    shippingMethod: Option[ShippingMethod]): AdjustmentResult =

    Xor.Left(OfferRejectionFailure(this, order.refNum, rejectionReason).single)
}
