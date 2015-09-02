package utils

import cats.data.{Validated, ValidatedNel}
import cats.data.Validated.{invalidNel, valid}
import cats.data.NonEmptyList
import com.wix.accord.{validate => runValidation, Failure => AccordFailure, GroupViolation, RuleViolation,
Violation, Validator}
import com.wix.accord.transform.ValidationTransform
import com.wix.accord
import services.ValidationFailure
import utils.Validation.Result.{Failure, Success}
import com.wix.accord.combinators._
import cats.implicits._

trait Validation[T] { this: T ⇒
  import Validation._

  def validator: ValidationTransform.TransformedValidator[T]

  def validate: Result = {
    val accordResult = runValidation(this)(validator)
    Result.fromAccord(accordResult)
  }

  def isValid: Boolean = { validate.isValid }
}

object Validation {
  import Result._

  sealed trait Result {

    import Result._

    // A catamorphism that runs a Success fn, s, or a Failure fn, f.
    final def fold[A](s: => A, f: Set[Violation] => A): A = {
      this match {
        case Success => s
        case Result.Failure(v) => f(v)
      }
    }

    final def messages: Set[String] = {
      fold(Set.empty,
        _.map { v => v.description.getOrElse("") ++ " " ++ v.constraint })
    }

    def isValid: Boolean
    def isInvalid: Boolean = !isValid
  }

  object Result {

    case object Success extends Result {
      def isValid = true
    }

    final case class Failure(violations: Set[Violation]) extends Result {
      def isValid = false
    }

    def fromAccord(r: accord.Result): Result = r match {
      case accord.Failure(violations) => Failure(violations)
      case accord.Success => Success
    }
  }

  private def toValidatedNel(constraint: String, r: accord.Result): ValidatedNel[String, Unit] = r match {
    case accord.Failure(f)  ⇒
      val errors = f.toList.map {
        case RuleViolation(_, err, _) ⇒ s"$constraint $err"
        case _ ⇒ "unknown error"
      }

      Validated.Invalid(NonEmptyList(errors.headOption.getOrElse("unknown error"), errors.tail))

    case accord.Success     ⇒
      valid({})
  }

  def notEmpty[A <: AnyRef <% HasEmpty](a: A, constraint: String): ValidatedNel[String, Unit] =
    toValidatedNel(constraint, new NotEmpty[A].apply(a))

  def matches(value: String, regex: String, constraint: String): ValidatedNel[String, Unit] =
    toValidatedNel(constraint, new MatchesRegex(regex.r.pattern, partialMatchAllowed = false).apply(value))

  def lesserThan(a: Int, size: Int, constraint: String): ValidatedNel[String, Unit] =
    toValidatedNel(constraint, new LesserThan[Int](size, "got").apply(a))

  def lesserThanOrEqual(a: Int, size: Int, constraint: String): ValidatedNel[String, Unit] =
    toValidatedNel(constraint, new LesserThanOrEqual[Int](size, "got").apply(a))

  def greaterThan(a: Int, size: Int, constraint: String): ValidatedNel[String, Unit] =
    toValidatedNel(constraint, new GreaterThan[Int](size, "got").apply(a))

  def greaterThanOrEqual(a: Int, size: Int, constraint: String): ValidatedNel[String, Unit] =
    toValidatedNel(constraint, new GreaterThanOrEqual[Int](size, "got").apply(a))
}
