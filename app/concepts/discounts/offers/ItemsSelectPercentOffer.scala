package concepts.discounts.offers

import concepts.discounts._

final case class ItemsSelectPercentOffer(discount: Int, references: Seq[ReferenceTuple]) extends Offer {

  val promoType: PromoType = ItemsPromo
  val offerType: OfferType = ItemsSinglePercentOff
}
