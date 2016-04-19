package concepts.discounts.qualifiers

import cats.data.Xor
import failures._
import models.order.Order
import models.order.lineitems.OrderLineItemProductData
import models.shipping.ShippingMethod
import org.json4s._

trait Qualifier {

  def check(order: Order, lineItems: Seq[OrderLineItemProductData],
    shippingMethod: Option[ShippingMethod]): Xor[Failures, Unit]
}

object Qualifier {

  case class QualifierFormat(qualifierType: QualifierType, attributes: JObject)
  type QualifierAstFormat = Seq[QualifierFormat]

}
