package models.discount.qualifiers

import cats.data.Xor
import failures.Failures
import cats.data.NonEmptyList
import cats.std.list._
import models.discount.DiscountInput
import services.Result
import utils.aliases._

case class AndQualifier(qualifiers: Seq[Qualifier]) extends Qualifier {

  def check(input: DiscountInput)(implicit ec: EC, es: ES): Result[Unit] = {
    reject(input, "Not implemented")
    /*
    val checks = qualifiers.map(_.check(order, lineItems, shippingMethod))
    val failures = checks.flatMap(_.fold(fs ⇒ fs.unwrap, q ⇒ Seq.empty))

    failures match {
      case head :: tail ⇒ Xor.Left(NonEmptyList(head, tail))
      case Nil          ⇒ Xor.Right(Unit)
    }
    */
  }
}
