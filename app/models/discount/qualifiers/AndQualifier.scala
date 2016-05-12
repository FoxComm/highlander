package models.discount.qualifiers

import scala.concurrent.Future

import cats.data.Xor
import cats.data.NonEmptyList
import cats.std.list._
import models.discount.DiscountInput
import services.Result
import utils.aliases._

case class AndQualifier(qualifiers: Seq[Qualifier]) extends Qualifier {

  def check(input: DiscountInput)(implicit ec: EC, es: ES): Result[Unit] = {
    val checks = Future.sequence(qualifiers.map(_.check(input)))

    checks.map(seq ⇒ seq.flatMap(_.fold(fs ⇒ fs.unwrap, q ⇒ Seq.empty))).map {
      case head :: tail ⇒ Xor.Left(NonEmptyList(head, tail))
      case Nil          ⇒ Xor.Right(Unit)
    }
  }
}
