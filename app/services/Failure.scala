package services

import com.stripe.exception.StripeException
import utils.Validation

sealed trait Failure
case class NotFoundFailure(message: String) extends Failure
case class StripeFailure(exception: StripeException) extends Failure
case class ValidationFailure(violation: Validation.Result.Failure) extends Failure
case class GeneralFailure[A](a: A) extends Failure
