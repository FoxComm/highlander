package phoenix.models.discount.offers

import cats.implicits._
import failures._
import phoenix.models.cord.lineitems.CartLineItemAdjustment
import phoenix.models.cord.lineitems.CartLineItemAdjustment._
import phoenix.models.discount._
import phoenix.models.discount.offers.Offer.OfferResult
import phoenix.utils.ElasticsearchApi._
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis

// Percent off all matched items in cart
case class ItemsPercentOffer(discount: Long, search: Seq[ProductSearch])
    extends Offer
    with PercentOffer
    with NonEmptySearch
    with ItemsOffer {

  val offerType: OfferType           = ItemsPercentOff
  val adjustmentType: AdjustmentType = LineItemAdjustment

  def adjust(input: DiscountInput)(implicit db: DB, ec: EC, apis: Apis, au: AU): OfferResult =
    if (discount > 0 && discount < 100) adjustInner(input)(search) else pureResult()

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
