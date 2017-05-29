package phoenix.models.discount.offers

import cats.implicits._
import core.db.Result
import core.failures._
import phoenix.models.discount._
import phoenix.models.discount.offers.Offer.OfferResult
import phoenix.utils.ElasticsearchApi._
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis

// Amount off single item
case class ItemAmountOffer(discount: Long, search: Seq[ProductSearch])
    extends Offer
    with AmountOffer
    with NonEmptySearch
    with ItemsOffer {

  val offerType: OfferType = ItemAmountOff

  def adjust(input: DiscountInput)(implicit db: DB, ec: EC, apis: Apis): Result[Seq[OfferResult]] =
    if (discount > 0) adjustInner(input)(search) else pureResult()

  def matchEither(input: DiscountInput)(
      either: Either[Failures, Buckets]): Either[Failures, Seq[OfferResult]] =
    either match {
      case Right(buckets) ⇒
        val matchedFormIds = buckets.filter(_.docCount > 0).map(_.key)
        input.lineItems.find(data ⇒ matchedFormIds.contains(data.productId.toString)) match {
          case Some(data) ⇒
            buildEither(input, subtract(data.price, discount), data.lineItemReferenceNumber.some)
          case _ ⇒ pureEither()
        }
      case _ ⇒ pureEither()
    }
}
