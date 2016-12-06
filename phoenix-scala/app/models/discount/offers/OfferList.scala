package models.discount.offers

import scala.concurrent.Future

import cats.data.{NonEmptyList, Xor}
import cats.instances.list._
import failures._
import models.cord.lineitems.OrderLineItemAdjustment
import models.cord.lineitems.OrderLineItemAdjustment._
import models.discount.DiscountInput
import models.discount.offers.Offer.OfferResult
import utils.aliases._

case class OfferList(offers: Seq[Offer]) extends Offer {

  val offerType: OfferType           = ListCombinator
  val adjustmentType: AdjustmentType = Combinator

  def adjust(input: DiscountInput)(implicit db: DB, ec: EC, es: ES, au: AU): OfferResult = {
    val adjustmentAttempts = Future.sequence(offers.map(_.adjust(input)))

    val adjustments = adjustmentAttempts.map(seq ⇒
          seq.flatMap { o ⇒
        o.fold(f ⇒ Seq.empty, qs ⇒ qs.flatMap(q ⇒ Seq(q)))
    })

    val failures =
      adjustmentAttempts.map(seq ⇒ seq.flatMap(o ⇒ o.fold(fs ⇒ fs.toList, q ⇒ Seq.empty)))

    val tupled: Future[(Seq[OrderLineItemAdjustment], Seq[Failure])] = {
      for (v1 ← adjustments; v2 ← failures) yield (v1, v2)
    }

    tupled.map {
      case (_, head :: tail) ⇒ Xor.Left(NonEmptyList(head, tail))
      case (adj, Nil)        ⇒ Xor.Right(adj)
    }
  }
}
