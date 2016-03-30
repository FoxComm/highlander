package concepts.discounts.offers

import concepts.discounts._

final case class ItemsSinglePercentOffer(discount: Int, referenceId: Int, referenceType: ReferenceType) extends Offer {

  val promoType: PromoType = ItemsPromo
  val offerType: OfferType = ItemsSinglePercentOff
}
