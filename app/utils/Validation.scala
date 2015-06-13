package utils

import org.scalactic._
import org.scalactic.Accumulation._
import com.wix.accord.{validate => runValidation, Success}
import com.wix.accord._

trait Validation {
  def validator[T]: Validator[T]
  def validate: Result = { runValidation(this)(validator) }
  def isValid: Boolean = { validate == Success }

  def validationFailures: Set[ErrorMessage] = {
    this.validate match {
      case Success =>
        Set.empty
      case Failure(violations) =>
        violations.map(Validation.formatViolation)
    }
  }
}

object Validation {
  def validationFailureToSet(failure: Failure): Set[ErrorMessage] = {
    failure.violations.map(formatViolation)
  }

  def formatViolation(v: Violation): String = v.description.getOrElse("") ++ " " ++ v.constraint
}