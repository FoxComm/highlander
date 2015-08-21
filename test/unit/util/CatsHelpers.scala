package util

import scala.reflect.runtime.universe._

import cats.data.{Validated, Xor}
import org.scalatest.Suite

trait CatsHelpers { this: Suite ⇒ /** For fail */
  def rightValue[A, B](xor: A Xor B)
                      (implicit ttA: TypeTag[A], ttB: TypeTag[B]): B =
    xor.fold(
      l ⇒ fail(s"Expected Right[${ ttB.tpe.dealias }], got Left[${ ttA.tpe.dealias }]: $l"),
      r ⇒ r
    )

  def leftValue[A, B](xor: A Xor B)
                     (implicit ttA: TypeTag[A], ttB: TypeTag[B]): A =
    xor.fold(
      l ⇒ l,
      r ⇒ fail(s"Expected Left[${ ttA.tpe.dealias }], got Right[${ ttB.tpe.dealias }]: $r")
    )

  def validValue[E, A](validated: Validated[E, A])
                      (implicit ttA: TypeTag[A], ttE: TypeTag[E]): A =
    validated.fold(
      e ⇒ fail(s"Expected Valid[${ ttA.tpe.dealias }], got Invalid[${ ttE.tpe.dealias }]: $e"),
      a ⇒ a
    )

  def invalidValue[E, A](validated: Validated[E, A])
                        (implicit ttA: TypeTag[A], ttE: TypeTag[E]): E =
    validated.fold(
      e ⇒ e,
      a ⇒ fail(s"Expected Invalid[${ ttA.tpe.dealias }], got Valid[${ ttE.tpe.dealias }]: $a")
    )
}
