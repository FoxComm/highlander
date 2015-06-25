import org.scalactic.{Bad, Good, Or}

import scalaz.NonEmptyList

/** Warnings */
sealed trait Warning
case object LegacyParameter extends Warning

/** Need a better name */
sealed trait MaybeWarnings[A]
case class NoWarnings[A](value: A) extends MaybeWarnings[A]
case class Warnings[A](value: A, warnings: NonEmptyList[Warning]) extends MaybeWarnings[A]

/** Failures */
sealed trait Failure
case object NotFound extends Failure

type Result[A] = MaybeWarnings[A] Or Failure

implicit class GoodExtensions[A](good: Good[A, Failure]) {
  def noWarnings    = good.map(NoWarnings(_))
  def withWarning(warning: Warning) = good.map(Warnings(_, NonEmptyList(warning)))
}

object SomeService {
  def doSomething(legacyParameter: Boolean = false): Result[Int] = {
    if (false) {
      Bad(NotFound)
    } else if (legacyParameter) {
      Good(42).withWarning(LegacyParameter)
    } else {
      Good(42).noWarnings
    }
  }
}

SomeService.doSomething(false) /** Good(Warnings(42,List())) */
SomeService.doSomething(true)  /** Good(Warnings(42,NonEmptyList(LegacyParameter))) */
