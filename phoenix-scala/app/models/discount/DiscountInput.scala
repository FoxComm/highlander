package models.discount

import models.cord.Cart
import models.cord.lineitems._
import models.objects.ObjectShadow
import models.product.Mvp
import models.shipping.ShippingMethod

/*
 * Gift card line items must be excluded from any kind or discount application, which means:
 * - they don't contribute to eligible for discount number of items
 * - they don't contribute to eligible for discount subtotal
 * - discounts can't be applied to a cart that only has gift card line items
 */

case class DiscountInput(promotion: ObjectShadow,
                         cart: Cart,
                         lineItems: Seq[LineItemProductData[_]],
                         shippingMethod: Option[ShippingMethod]) {

  val eligibleForDiscountSubtotal: Int = lineItems.collect {
    case li if li.isEligibleForDiscount ⇒
      Mvp.priceAsInt(li.productVariantForm, li.productVariantShadow)
  }.sum

  val eligibleForDiscountNumItems: Int = lineItems.count(_.isEligibleForDiscount)

  val isEligibleForDiscount: Boolean =
    eligibleForDiscountNumItems > 0
}
