package phoenix.models.discount

/*
 * Gift card line items must be excluded from any kind or discount application, which means:
 * - they don't contribute to eligible for discount number of items
 * - they don't contribute to eligible for discount subtotal
 * - discounts can't be applied to a cart that only has gift card line items
 */

// Dq stands for "discount qualifier". To avoid naming collisions
sealed trait DqLineItemType
case object DqGiftCardLineItem extends DqLineItemType
case object DqRegularLineItem  extends DqLineItemType

case class DqLineItem(skuCode: String,
                      productId: Int,
                      price: Long,
                      lineItemType: DqLineItemType,
                      lineItemReferenceNumber: String) {

  def isEligibleForDiscount: Boolean =
    lineItemType == DqRegularLineItem
}

case class DiscountInput(promotionShadowId: Int,
                         cartRefNum: String,
                         customerAccountId: Int,
                         lineItems: Seq[DqLineItem],
                         shippingCost: Option[Long]) {

  val eligibleForDiscountSubtotal: Long = lineItems.collect {
    case li if li.isEligibleForDiscount ⇒ li.price
  }.sum

  val eligibleForDiscountNumItems: Int = lineItems.count(_.isEligibleForDiscount)

  val isEligibleForDiscount: Boolean =
    eligibleForDiscountNumItems > 0
}
