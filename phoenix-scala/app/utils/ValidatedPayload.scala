package utils

import cats.data.Validated._
import failures.Failures

case class FoxValidationException(failures: Failures) extends Exception

/*
 * Super-fast mechanism for payload validation. If payload is invalid, we should not even proceed to services.
 * Throwing an exception:
 * 1. does not require wrapping payload type into `Validated`
 * 2. happens before any DB transaction is initiated, so it's pretty safe
 * 2. protects you from forgetting to call `.validate`. We don't have exceptions to payload validation after all!
 */
trait ValidatedPayload[A] extends Validation[A] { self: A ⇒

  validate match {
    case Valid(_)          ⇒ Unit
    case Invalid(failures) ⇒ throw FoxValidationException(failures)
  }
}
