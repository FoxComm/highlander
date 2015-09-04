package utils

import cats.data.Validated.{valid, invalidNel}
import cats.data.{NonEmptyList, Validated, ValidatedNel}
import com.wix.accord
import com.wix.accord.combinators._
import com.wix.accord.transform.ValidationTransform
import com.wix.accord.{Failure ⇒ AccordFailure, RuleViolation, Violation, validate ⇒ runValidation}
import org.joda.time.DateTime
import services._

trait ValidationNew[T] { this: T ⇒
  def validate: ValidatedNel[Failure, T]
}

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
}

object Checks {
  def validExpr(expression: Boolean, message: String): ValidatedNel[Failure, Unit] = expression match {
    case false ⇒ invalidNel(GeneralFailure(message))
    case _     ⇒ valid({})
  }

  def invalidExpr(expression: Boolean, message: String): ValidatedNel[Failure, Unit] = expression match {
    case true ⇒ invalidNel(GeneralFailure(message))
    case _    ⇒ valid({})
  }

  def notEmpty[A <: AnyRef <% HasEmpty](a: A, constraint: String): ValidatedNel[Failure, Unit] =
    toValidatedNel(constraint, new NotEmpty[A].apply(a))

  def notEmptyIf[A <: AnyRef <% HasEmpty](a: A, expression: Boolean, constraint: String): ValidatedNel[Failure, Unit] = {
    expression match {
      case true ⇒ notEmpty(a, constraint)
      case _    ⇒ valid({})
    }
  }

  def notExpired(expYear: Int, expMonth: Int, message: String): ValidatedNel[Failure, Unit] = {
    val today = DateTime.now()
    val expDate = new DateTime(expYear, expMonth, 1, 0, 0).plusMonths(1).minusSeconds(1)

    expDate.isEqual(today) || expDate.isAfter(today) match {
      case false ⇒ invalidNel(GeneralFailure(message))
      case _     ⇒ valid({})
    }
  }

  def withinNumberOfYears(expYear: Int, expMonth: Int, numYears: Int, message: String): ValidatedNel[Failure, Unit] = {
    val today = DateTime.now()
    val expDate = new DateTime(expYear, expMonth, 1, 0, 0).plusMonths(1).minusSeconds(1)

    expDate.isBefore(today.plusYears(numYears)) match {
      case false ⇒ invalidNel(GeneralFailure(message))
      case _     ⇒ valid({})
    }
  }

  def matches(value: String, regex: String, constraint: String): ValidatedNel[Failure, Unit] =
    toValidatedNel(constraint, new MatchesRegex(regex.r.pattern, partialMatchAllowed = false).apply(value))

  def lesserThan(value: Int, limit: Int, constraint: String): ValidatedNel[Failure, Unit] =
    toValidatedNel(constraint, new LesserThan[Int](limit, "got").apply(value))

  def lesserThanOrEqual(value: Int, limit: Int, constraint: String): ValidatedNel[Failure, Unit] =
    toValidatedNel(constraint, new LesserThanOrEqual[Int](limit, "got").apply(value))

  def greaterThan(value: Int, limit: Int, constraint: String): ValidatedNel[Failure, Unit] =
    toValidatedNel(constraint, new GreaterThan[Int](limit, "got").apply(value))

  def greaterThanOrEqual(value: Int, limit: Int, constraint: String): ValidatedNel[Failure, Unit] =
    toValidatedNel(constraint, new GreaterThanOrEqual[Int](limit, "got").apply(value))

  private def toValidatedNel(constraint: String, r: accord.Result): ValidatedNel[Failure, Unit] = r match {
    case accord.Failure(f) ⇒
      val errors = f.toList.map {
        case RuleViolation(_, err, _) ⇒ GeneralFailure(s"$constraint $err")
        case _ ⇒ GeneralFailure("unknown error")
      }

      Validated.Invalid(NonEmptyList(errors.headOption.getOrElse(GeneralFailure("unknown error")), errors.tail))

    case accord.Success ⇒
      valid({})
  }
}
