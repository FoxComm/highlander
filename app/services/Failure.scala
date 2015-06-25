package services

import collection.immutable
import com.stripe.exception.StripeException
import utils.Validation

sealed trait Failure {
  def description: immutable.Traversable[String]
}

case class NotFoundFailure(message: String) extends Failure {
  override def description = List(message)
}

case class StripeFailure(exception: StripeException) extends Failure {
  override def description = List(exception.getMessage)
}

case class ValidationFailure(violation: Validation.Result.Failure) extends Failure {
  override def description = violation.messages.map(_.toString)
}

case class GeneralFailure(a: String) extends Failure {
  override def description = List(a)
}
