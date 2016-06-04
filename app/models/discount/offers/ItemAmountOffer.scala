package models.discount.offers

import cats.implicits._
import cats.data.Xor
import failures._
import models.discount._
import models.discount.offers.Offer.OfferResult
import models.order.lineitems.OrderLineItemAdjustment
import models.order.lineitems.OrderLineItemAdjustment._
import utils.ElasticsearchApi._
import utils.aliases._

// Amount off single item
case class ItemAmountOffer(discount: Int, search: Seq[ProductSearch])
    extends Offer
    with AmountOffer
    with ItemsOffer {

  val offerType: OfferType           = ItemAmountOff
  val adjustmentType: AdjustmentType = LineItemAdjustment

  def adjust(input: DiscountInput)(implicit db: DB, ec: EC, es: ES): OfferResult =
    if (discount > 0) adjustInner(input)(search) else pureResult()

  def matchXor(input: DiscountInput)(
      xor: Failures Xor Buckets): Failures Xor Seq[OrderLineItemAdjustment] =
    xor match {
      case Xor.Right(buckets) ⇒
        val matchedFormIds = buckets.filter(_.docCount > 0).map(_.key)
        input.lineItems.find(data ⇒ matchedFormIds.contains(data.product.formId.toString)) match {
          case Some(data) ⇒
            buildXor(input, substract(price(data), discount), data.lineItem.referenceNumber.some)
          case _ ⇒ pureXor()
        }
      case _ ⇒ pureXor()
    }
}
