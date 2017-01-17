package testutils

import scala.reflect.runtime.universe._

import cats.data.{Validated, Xor}
import org.scalatest.Assertions

trait CatsHelpers extends Assertions {

  def rightValue[A, B](xor: A Xor B)(implicit ttA: TypeTag[A], ttB: TypeTag[B]): B =
    xor.fold(
      l ⇒ fail(s"Expected Right[${ttB.tpe.dealias}], got Left[${ttA.tpe.dealias}]: $l"),
      r ⇒ r
    )

  def leftValue[A, B](xor: A Xor B)(implicit ttA: TypeTag[A], ttB: TypeTag[B]): A =
    xor.fold(
      l ⇒ l,
      r ⇒ fail(s"Expected Left[${ttA.tpe.dealias}], got Right[${ttB.tpe.dealias}]: $r")
    )

  def validValue[E, A](validated: Validated[E, A])(implicit ttA: TypeTag[A], ttE: TypeTag[E]): A =
    validated.fold(
      e ⇒ fail(s"Expected Valid[${ttA.tpe.dealias}], got Invalid[${ttE.tpe.dealias}]: $e"),
      a ⇒ a
    )

  def invalidValue[E, A](validated: Validated[E, A])(implicit ttA: TypeTag[A],
                                                     ttE: TypeTag[E]): E =
    validated.fold(
      e ⇒ e,
      a ⇒ fail(s"Expected Invalid[${ttA.tpe.dealias}], got Valid[${ttE.tpe.dealias}]: $a")
    )

  implicit class ImplicitCatsHelpersXor[A, B](xor: A Xor B) {
    def rightVal(implicit ttA: TypeTag[A], ttB: TypeTag[B]) = rightValue(xor)
    def leftVal(implicit ttA: TypeTag[A], ttB: TypeTag[B])  = leftValue(xor)
  }

  implicit class ImplicitCatsHelpersValidated[E, A](validated: Validated[E, A]) {
    def validVal(implicit ttA: TypeTag[A], ttE: TypeTag[E]) =
      validValue(validated)
    def invalidVal(implicit ttA: TypeTag[A], ttE: TypeTag[E]) =
      invalidValue(validated)
  }
}
