package concepts.discounts.offers

import cats.data.Xor
import concepts.discounts.{LineItemAdjustment, OfferType}
import concepts.discounts.offers.Offer.AdjustmentResult
import failures._
import models.order.Order
import models.order.lineitems.OrderLineItemProductData
import models.shipping.ShippingMethod
import org.json4s._

trait Offer {

  def adjust(order: Order, lineItems: Seq[OrderLineItemProductData],
    shippingMethod: Option[ShippingMethod]): AdjustmentResult
}

object Offer {

  final case class OfferFormat(offerType: OfferType, attributes: JObject)
  type OfferAstFormat = Seq[OfferFormat]

  type AdjustmentResult = Xor[Failures, Seq[LineItemAdjustment]]
}