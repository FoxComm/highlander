package utils

import java.time.LocalDateTime

import scala.util.matching.Regex

import cats.data.Validated.{Invalid, Valid, invalidNel, valid}
import cats.data.{NonEmptyList, Validated, ValidatedNel}
import com.wix.accord
import com.wix.accord.RuleViolation
import com.wix.accord.combinators._
import failures.{Failure, GeneralFailure}

trait Validation[M] {
  def validate: ValidatedNel[Failure, M]
}

object Validation {
  val prefix = "got"

  val ok: ValidatedNel[Failure, Unit] = valid(Unit)

  def validExpr(expression: Boolean, message: ⇒ String): ValidatedNel[Failure, Unit] =
    if (expression) valid({})
    else invalidNel(GeneralFailure(message))

  def invalidExpr(expression: Boolean, message: ⇒ String): ValidatedNel[Failure, Unit] =
    if (expression) invalidNel(GeneralFailure(message))
    else valid({})

  def notEmpty[A <: AnyRef <% HasEmpty](a: A, constraint: String): ValidatedNel[Failure, Unit] =
    toValidatedNel(constraint, new NotEmpty[A].apply(a))

  def notEmptyIf[A <: AnyRef <% HasEmpty](a: A,
                                          expression: Boolean,
                                          constraint: String): ValidatedNel[Failure, Unit] =
    if (expression) notEmpty(a, constraint)
    else valid({})

  def nullOrNotEmpty[A <: AnyRef <% HasEmpty](a: Option[A],
                                              constraint: String): ValidatedNel[Failure, Unit] = {
    a.fold(ok) { s ⇒
      notEmpty(s, constraint)
    }
  }

  def emailish(maybeEmail: String, fieldName: String): ValidatedNel[Failure, Unit] =
    validExpr(maybeEmail.contains('@'), s"$fieldName must be an email")

  def notExpired(expYear: Int, expMonth: Int, message: String): ValidatedNel[Failure, Unit] = {
    val today = LocalDateTime.now()

    val validDate = Validated.catchOnly[java.time.DateTimeException] {
      LocalDateTime.of(expYear, expMonth, 1, 0, 0).plusMonths(1).minusSeconds(1)
    }

    validDate match {
      case Valid(expDate) if expDate.isEqual(today) || expDate.isAfter(today) ⇒
        valid(Unit)

      case Invalid(e) ⇒
        invalidNel(GeneralFailure(e.getMessage))

      case _ ⇒
        invalidNel(GeneralFailure(message))
    }
  }

  def withinNumberOfYears(expYear: Int,
                          expMonth: Int,
                          numYears: Int,
                          message: String): ValidatedNel[Failure, Unit] = {
    val today = LocalDateTime.now()

    val validDate = Validated.catchOnly[java.time.DateTimeException] {
      LocalDateTime.of(expYear, expMonth, 1, 0, 0).plusMonths(1).minusSeconds(1)
    }

    validDate match {
      case Valid(expDate) ⇒
        if (expDate.isBefore(today.plusYears(numYears.toLong)))
          valid(Unit)
        else
          invalidNel(GeneralFailure(message))

      case Invalid(e) ⇒
        invalidNel(GeneralFailure(e.getMessage))

      case _ ⇒
        invalidNel(GeneralFailure(message))
    }
  }

  // valid credit cards for us cannot have more than 20 years expiration from this year
  def withinTwentyYears(year: Int, message: String): ValidatedNel[Failure, Unit] = {
    val today = LocalDateTime.now()
    val expDate =
      LocalDateTime.of(year, today.getMonthValue, 1, 0, 0).plusMonths(1).minusSeconds(1)
    val msg = message ++ s" year should be between ${today.getYear} and ${expDate.getYear}"

    withinNumberOfYears(year, today.getMonthValue, 20, msg)
  }

  def matches(value: String, regex: Regex, constraint: String): ValidatedNel[Failure, Unit] =
    toValidatedNel(constraint,
                   new MatchesRegex(regex.pattern, partialMatchAllowed = false).apply(value))

  def matches(value: String, regex: String, constraint: String): ValidatedNel[Failure, Unit] =
    toValidatedNel(constraint,
                   new MatchesRegex(regex.r.pattern, partialMatchAllowed = false).apply(value))

  def between(value: Int,
              lowerBound: Int,
              upperBound: Int,
              constraint: String): ValidatedNel[Failure, Unit] =
    toValidatedNel(constraint,
                   new InRangeInclusive[Int](lowerBound, upperBound, prefix).apply(value))

  def isMonth(month: Int, constraint: String): ValidatedNel[Failure, Unit] =
    toValidatedNel(s"$constraint month", new InRangeInclusive[Int](1, 12, prefix).apply(month))

  def lesserThan(value: Int, limit: Int, constraint: String): ValidatedNel[Failure, Unit] =
    toValidatedNel(constraint, new LesserThan[Int](limit, prefix).apply(value))

  def lesserThanOrEqual(value: Int, limit: Int, constraint: String): ValidatedNel[Failure, Unit] =
    toValidatedNel(constraint, new LesserThanOrEqual[Int](limit, prefix).apply(value))

  def greaterThan(value: Int, limit: Int, constraint: String): ValidatedNel[Failure, Unit] =
    toValidatedNel(constraint, new GreaterThan[Int](limit, prefix).apply(value))

  def greaterThanOrEqual(value: Int, limit: Int, constraint: String): ValidatedNel[Failure, Unit] =
    toValidatedNel(constraint, new GreaterThanOrEqual[Int](limit, prefix).apply(value))

  private def toValidatedNel(constraint: String, r: accord.Result): ValidatedNel[Failure, Unit] =
    r match {
      case accord.Failure(f) ⇒
        val errors = f.toList.map {
          case RuleViolation(_, err, _) ⇒ GeneralFailure(s"$constraint $err")
          case _                        ⇒ GeneralFailure("unknown error")
        }

        Validated.Invalid(
            NonEmptyList(errors.headOption.getOrElse(GeneralFailure("unknown error")),
                         errors.tail))

      case accord.Success ⇒
        valid({})
    }
}
