package models.discount.offers

import scala.concurrent.Future

import cats.data.Xor
import cats.data.NonEmptyList
import cats.std.list._
import failures._
import models.order.lineitems._
import models.discount.DiscountInput
import models.order.lineitems.OrderLineItemAdjustment._
import services.Result
import utils.aliases._

case class OfferList(offers: Seq[Offer]) extends Offer {

  val adjustmentType: AdjustmentType = ComplexAdjustment

  def adjust(input: DiscountInput)(implicit ec: EC, es: ES): Result[Seq[OrderLineItemAdjustment]] = {
    val adjustmentAttempts = Future.sequence(offers.map(_.adjust(input)))

    val adjustments = adjustmentAttempts.map(seq ⇒ seq.flatMap {
      o ⇒ o.fold(f ⇒ Seq.empty, qs ⇒ qs.flatMap(q ⇒ Seq(q)))
    })

    val failures = adjustmentAttempts.map(seq ⇒ seq.flatMap(o ⇒ o.fold(fs ⇒ fs.unwrap, q ⇒ Seq.empty)))

    val tupled: Future[(Seq[OrderLineItemAdjustment], Seq[Failure])] = {
      for (v1 <- adjustments; v2 <- failures) yield (v1, v2)
    }

    tupled.map {
      case (adj, head :: tail) ⇒ Xor.Left(NonEmptyList(head, tail))
      case (adj, Nil)          ⇒ Xor.Right(adj)
    }
  }
}
