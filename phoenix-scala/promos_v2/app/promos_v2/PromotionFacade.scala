case class CartMetadata(lineItems: NonEmptyList[LineItemMetadata],
                        shippingCost: Int, // no need for option, just zero
                       total: Int, // grand-? sub-?
                       customerGroupIds: List[Int],
                        appliedCouponId: Int
                       )
/*
Reason for having both prices in line items, shipping cost and grand total is
that different qualifiers check different things.
Promotions module can't assume that grand total equals to sum of line item prices plus shipping.
 */
/*
Note on appliedCouponId:
- use it for exclusive/non-exclusive promo qualification
- may become List[Int] if we allow multiple
 */

case class LineItemMetadata(referenceNumber:String,
                            unitPrice: Int,
                            quantity: Int,
                            lineItemType: LineItemType,
                            productId: Int
                           )
/*
Possible changes to the above:
productId:
- can potentially become productIds for things like bento boxes
- can potentially be skuId in case we want to allow promotions for variants
In any case, if we get multiple types of metadata, they must be implemented as ADT, not optional attrs
 */

/*
Facade is an "API" for a module
This is the thing that's going to be tested, not services and not routes
 */
object PromotionFacade {

  def create() = ???

  def get() = ???

  def update() = ???

  def archive() = ???

  def bestAutoApply(cart: CartMetadata): OfferResponse = ???

}