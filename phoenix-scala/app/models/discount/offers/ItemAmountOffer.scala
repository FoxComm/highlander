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

// Amount off single item
case class ItemAmountOffer(discount: Int, search: Seq[ProductSearch])
    extends Offer
    with AmountOffer
    with NonEmptySearch
    with ItemsOffer {

  val offerType: OfferType           = ItemAmountOff
  val adjustmentType: AdjustmentType = LineItemAdjustment

  def adjust(input: DiscountInput)(implicit db: DB, ec: EC, apis: Apis, au: AU): OfferResult =
    if (discount > 0) adjustInner(input)(search) else pureResult()

  def matchEither(input: DiscountInput)(
      either: Either[Failures, Buckets]): Either[Failures, Seq[CartLineItemAdjustment]] =
    either match {
      case Right(buckets) ⇒
        val matchedFormIds = buckets.filter(_.docCount > 0).map(_.key)
        input.lineItems.find(data ⇒ matchedFormIds.contains(data.productForm.id.toString)) match {
          case Some(data) ⇒
            buildEither(input, subtract(price(data), discount), data.lineItemReferenceNumber.some)
          case _ ⇒ pureEither()
        }
      case _ ⇒ pureEither()
    }
}