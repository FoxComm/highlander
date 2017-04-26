package models.discount.offers

import cats.implicits._
import failures._
import io.circe.syntax._
import models.cord.lineitems.CartLineItemAdjustment
import models.cord.lineitems.CartLineItemAdjustment._
import models.discount._
import models.discount.offers.Offer.OfferResult
import utils.ElasticsearchApi._
import utils.aliases._
import utils.apis.Apis
import utils.json.codecs._

case class SetPriceOffer(setPrice: Int, numUnits: Int, search: Seq[ProductSearch])
    extends Offer
    with SetOffer
    with NonEmptySearch
    with ItemsOffer {

  val offerType: OfferType           = SetPrice
  val adjustmentType: AdjustmentType = LineItemAdjustment

  def adjust(input: DiscountInput)(implicit db: DB, ec: EC, apis: Apis, au: AU): OfferResult =
    if (setPrice > 0 && numUnits < 100) adjustInner(input)(search) else pureResult()

  def matchEither(input: DiscountInput)(
      xor: Either[Failures, Buckets]): Either[Failures, Seq[CartLineItemAdjustment]] =
    xor match {
      case Right(buckets) ⇒
        val matchedFormIds = buckets.filter(_.docCount > 0).map(_.key)
        val adjustments = input.lineItems
          .filter(data ⇒ matchedFormIds.contains(data.productForm.id.toString))
          .take(numUnits)
          .map { data ⇒
            build(input, subtract(price(data), setPrice), data.lineItemReferenceNumber.some)
          }

        Either.right(adjustments)
      case _ ⇒ pureEither()
    }

  def json: Json = this.asJson
}
