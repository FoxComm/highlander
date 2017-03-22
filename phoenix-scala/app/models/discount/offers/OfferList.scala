package models.discount.offers

import scala.concurrent.Future

import cats._
import cats.data._
import cats.implicits._
import failures._
import models.cord.lineitems.CartLineItemAdjustment
import models.cord.lineitems.CartLineItemAdjustment._
import models.discount.DiscountInput
import models.discount.offers.Offer.OfferResult
import utils.aliases._
import utils.db._

case class OfferList(offers: Seq[Offer]) extends Offer {

  val offerType: OfferType           = ListCombinator
  val adjustmentType: AdjustmentType = Combinator

  def adjust(input: DiscountInput)(implicit db: DB, ec: EC, es: ES, au: AU): OfferResult =
    Result.seqCollectFailures(offers.map(_.adjust(input)).toList).map(_.flatten)

}
