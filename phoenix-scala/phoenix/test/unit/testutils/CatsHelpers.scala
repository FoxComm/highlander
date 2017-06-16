package testutils

import cats.data.Validated
import org.scalatest.Assertions

import scala.reflect.runtime.universe._

trait CatsHelpers extends Assertions {

  def rightValue[A, B](either: Either[A, B])(implicit ttA: TypeTag[A], ttB: TypeTag[B]): B =
    either.fold(
      l ⇒ fail(s"Expected Right[${ttB.tpe.dealias}], got Left[${ttA.tpe.dealias}]: $l"),
      r ⇒ r
    )

  def leftValue[A, B](either: Either[A, B])(implicit ttA: TypeTag[A], ttB: TypeTag[B]): A =
    either.fold(
      l ⇒ l,
      r ⇒ fail(s"Expected Left[${ttA.tpe.dealias}], got Right[${ttB.tpe.dealias}]: $r")
    )

  def validValue[E, A](validated: Validated[E, A])(implicit ttA: TypeTag[A], ttE: TypeTag[E]): A =
    validated.fold(
      e ⇒ fail(s"Expected Valid[${ttA.tpe.dealias}], got Invalid[${ttE.tpe.dealias}]: $e"),
      a ⇒ a
    )

  def invalidValue[E, A](validated: Validated[E, A])(implicit ttA: TypeTag[A], ttE: TypeTag[E]): E =
    validated.fold(
      e ⇒ e,
      a ⇒ fail(s"Expected Invalid[${ttA.tpe.dealias}], got Valid[${ttE.tpe.dealias}]: $a")
    )

  implicit class ImplicitCatsHelpersEither[A, B](either: Either[A, B]) {
    def rightVal(implicit ttA: TypeTag[A], ttB: TypeTag[B]): B = rightValue(either)
    def leftVal(implicit ttA: TypeTag[A], ttB: TypeTag[B]): A  = leftValue(either)
  }

  implicit class ImplicitCatsHelpersValidated[E, A](validated: Validated[E, A]) {
    def validVal(implicit ttA: TypeTag[A], ttE: TypeTag[E]): A =
      validValue(validated)
    def invalidVal(implicit ttA: TypeTag[A], ttE: TypeTag[E]): E =
      invalidValue(validated)
  }
}
