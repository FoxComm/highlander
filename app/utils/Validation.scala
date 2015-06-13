package utils

import com.wix.accord.transform.ValidationTransform
import org.scalactic._
import com.wix.accord.{validate => runValidation, Success}
import com.wix.accord._

trait Validation[T] {
  def validator: ValidationTransform.TransformedValidator[T]

  def validate: Result = { runValidation(this.asInstanceOf[T])(validator.asInstanceOf[Validator[T]]) }

  def isValid: Boolean = { validate == Success }
}

object Validation {
  def validationFailureToSet(failure: Failure): Set[ErrorMessage] = {
    failure.violations.map(formatViolation)
  }

  def formatViolation(v: Violation): String = { v.description.getOrElse("") ++ " " ++ v.constraint }
}