package concepts.discounts.offers

import concepts.discounts._

case object FreeShippingOffer extends Offer {

  val promoType: PromoType = OrderPromo
  val offerType: OfferType = FreeShipping
}
