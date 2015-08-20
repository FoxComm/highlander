package util

import scala.reflect.runtime.universe._

import cats.data.{Validated, Xor}
import org.scalatest.Suite

trait CatsHelpers { this: Suite ⇒ /** For fail */
  def rightValue[A, B](xor: A Xor B)
                      (implicit ttA: TypeTag[A], ttB: TypeTag[B]): B =
    xor match {
      case Xor.Right(r) ⇒ r
      case Xor.Left(l)  ⇒
          fail(s"Expected Right[${ ttB.tpe.dealias }], got Left[${ ttA.tpe.dealias }]: $l")
    }

  def leftValue[A, B](xor: A Xor B)
                     (implicit ttA: TypeTag[A], ttB: TypeTag[B]): A =
    xor match {
      case Xor.Right(r) ⇒
        fail(s"Expected Left[${ ttA.tpe.dealias }], got Right[${ ttB.tpe.dealias }]: $r")
      case Xor.Left(l)  ⇒ l
    }

  def validValue[E, A](validated: Validated[E, A])
                      (implicit ttA: TypeTag[A], ttE: TypeTag[E]): A =
    validated match {
      case Validated.Valid(v)   ⇒ v
      case Validated.Invalid(e) ⇒
        fail(s"Expected Valid[${ ttA.tpe.dealias }], got Invalid[${ ttE.tpe.dealias }]: $e")
    }

  def invalidValue[E, A](validated: Validated[E, A])
                        (implicit ttA: TypeTag[A], ttE: TypeTag[E]): E =
    validated match {
      case Validated.Valid(v)   ⇒
        fail(s"Expected Invalid[${ ttA.tpe.dealias }], got Valid[${ ttE.tpe.dealias }]: $v")
      case Validated.Invalid(e) ⇒ e
    }
}
