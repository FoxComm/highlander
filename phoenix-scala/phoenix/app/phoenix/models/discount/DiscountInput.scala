package phoenix.models.discount

import models.objects.ObjectShadow
import phoenix.models.cord.Cart
import phoenix.models.cord.lineitems._
import phoenix.models.product.Mvp
import phoenix.models.shipping.ShippingMethod

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
      Mvp.priceAsInt(li.skuForm, li.skuShadow)
  }.sum

  val eligibleForDiscountNumItems: Int = lineItems.count(_.isEligibleForDiscount)

  val isEligibleForDiscount: Boolean =
    eligibleForDiscountNumItems > 0
}
