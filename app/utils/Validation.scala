package utils

import org.scalactic._
import com.wix.accord.{validate => runValidation, Success}
import com.wix.accord._

trait Validation {
  def validator[T]: Validator[T]
  def validate: Result = { runValidation(this)(validator) }
  def isValid: Boolean = { validate == Success }
}

object Validation {
  def validationFailureToSet(failure: Failure): Set[ErrorMessage] = {
    failure.violations.map(formatViolation)
  }

  def formatViolation(v: Violation): String = v.description.getOrElse("") ++ " " ++ v.constraint
}