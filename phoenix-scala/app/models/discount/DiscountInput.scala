package models.discount

import models.cord.Cart
import models.cord.lineitems._
import models.objects.ObjectShadow
import models.shipping.ShippingMethod

case class DiscountInput(promotion: ObjectShadow,
                         cart: Cart,
                         lineItems: Seq[OrderLineItemProductData],
                         shippingMethod: Option[ShippingMethod])
