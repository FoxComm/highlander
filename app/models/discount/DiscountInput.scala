package models.discount

import models.objects.ObjectShadow
import models.order.Order
import models.order.lineitems._
import models.shipping.ShippingMethod

case class DiscountInput(promotion: ObjectShadow,
                         order: Order,
                         lineItems: Seq[OrderLineItemProductData],
                         shippingMethod: Option[ShippingMethod])
