package concepts.discounts.offers

import concepts.discounts._

final case class OrderPercentOffer(discount: Int) extends Offer {

  val promoType: PromoType = OrderPromo
  val offerType: OfferType = OrderPercentOff
}
