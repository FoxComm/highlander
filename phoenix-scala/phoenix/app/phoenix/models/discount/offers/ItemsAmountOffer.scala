package phoenix.models.discount.offers

import cats.implicits._
import core.db.Result
import core.failures._
import phoenix.models.discount._
import phoenix.models.discount.offers.Offer.OfferResult
import phoenix.utils.ElasticsearchApi._
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis

// Amount off all matched items in cart
case class ItemsAmountOffer(discount: Int, search: Seq[ProductSearch])
    extends Offer
    with AmountOffer
    with NonEmptySearch
    with ItemsOffer {

  val offerType: OfferType = ItemsAmountOff

  def adjust(input: DiscountInput)(implicit db: DB,
                                   ec: EC,
                                   apis: Apis,
                                   au: AU): Result[Seq[OfferResult]] =
    if (discount > 0) adjustInner(input)(search) else pureResult()

  def matchEither(input: DiscountInput)(
      either: Either[Failures, Buckets]): Either[Failures, Seq[OfferResult]] =
    either match {
      case Right(buckets) ⇒
        val matchedFormIds = buckets.filter(_.docCount > 0).map(_.key)
        val offerResults = input.lineItems
          .filter(data ⇒ matchedFormIds.contains(data.productForm.id.toString))
          .map { data ⇒
            OfferResult(input,
                        subtract(price(data), discount),
                        data.lineItemReferenceNumber.some,
                        offerType)
          }

        Either.right(offerResults)
      case _ ⇒ pureEither()
    }
}
