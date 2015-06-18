package utils

import com.wix.accord.transform.ValidationTransform
import org.scalactic._
import org.scalactic.Accumulation._
import com.wix.accord.{validate => runValidation, Failure => AccordFailure, Violation, Validator}
import com.wix.accord

trait Validation[T] {
  import Validation._

  def validator: ValidationTransform.TransformedValidator[T]

  def validate: Result = {
    val accordResult = runValidation(this.asInstanceOf[T])(validator.asInstanceOf[Validator[T]])
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
        case Failure(v) => f(v)
      }
    }

    final def messages: Set[ErrorMessage] = {
      fold(Set.empty,
        _.map { v => v.description.getOrElse("") ++ " " ++ v.constraint })
    }

    def isValid: Boolean
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
