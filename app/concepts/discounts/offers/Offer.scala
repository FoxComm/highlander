package concepts.discounts.offers

import cats.data.Xor
import concepts.discounts.LineItemAdjustment
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

  type AdjustmentResult = Xor[Failures, Seq[LineItemAdjustment]]
  type OfferAstFormat = Map[String, JObject]
}