package models.discount.offers

import scala.concurrent.Future

import cats._
import cats.data._
import cats.implicits._
import cats.instances.list._
import failures._
import models.cord.lineitems.OrderLineItemAdjustment
import models.cord.lineitems.OrderLineItemAdjustment._
import models.discount.DiscountInput
import models.discount.offers.Offer.OfferResult
import utils.aliases._
import services.Result

case class OfferList(offers: Seq[Offer]) extends Offer {

  val offerType: OfferType           = ListCombinator
  val adjustmentType: AdjustmentType = Combinator

  def adjust(input: DiscountInput)(implicit db: DB, ec: EC, es: ES, au: AU): OfferResult =
    Result.sequenceJoiningFailures(offers.map(_.adjust(input))).map(_.flatten)

}
