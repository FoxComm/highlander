package models.discount.offers

import cats.implicits._
import failures._
import models.cord.lineitems.CartLineItemAdjustment
import models.cord.lineitems.CartLineItemAdjustment._
import models.discount._
import models.discount.offers.Offer.OfferResult
import utils.ElasticsearchApi._
import utils.aliases._
import utils.apis.Apis

// Amount off all matched items in cart
case class ItemsAmountOffer(discount: Int, search: Seq[ProductSearch])
    extends Offer
    with AmountOffer
    with NonEmptySearch
    with ItemsOffer {

  val offerType: OfferType           = ItemsAmountOff
  val adjustmentType: AdjustmentType = LineItemAdjustment

  def adjust(input: DiscountInput)(implicit db: DB, ec: EC, apis: Apis, au: AU): OfferResult =
    if (discount > 0) adjustInner(input)(search) else pureResult()

  def matchEither(input: DiscountInput)(
      either: Either[Failures, Buckets]): Either[Failures, Seq[CartLineItemAdjustment]] =
    either match {
      case Right(buckets) ⇒
        val matchedFormIds = buckets.filter(_.docCount > 0).map(_.key)
        val adjustments = input.lineItems
          .filter(data ⇒ matchedFormIds.contains(data.productForm.id.toString))
          .map { data ⇒
            build(input, subtract(price(data), discount), data.lineItemReferenceNumber.some)
          }

        Either.right(adjustments)
      case _ ⇒ pureEither()
    }
}