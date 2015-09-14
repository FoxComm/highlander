package utils

import scala.concurrent.Await
import scala.concurrent.duration.Duration.Inf
import scala.util.control.NoStackTrace

import com.stripe.exception.StripeException
import services.StripeRuntimeException
import util.TestBase

class WiredStripeApiTest extends TestBase {
  private val api = new WiredStripeApi

  "Wired Stripe API" - {
    "catches StripeException and returns a Result.failure" in {
      val result = api.inBlockingPool("abc")(_ ⇒ throw someStripeException)
      leftValue(result.futureValue).head must === (StripeRuntimeException(someStripeException))
    }

    "does not catch other exceptions" in {
      val result = api.inBlockingPool("abc")(_ ⇒ 42 / 0)

      /** Scalatest’s futureValue wraps the exception, so we can’t use it here. */
      an [ArithmeticException] must be thrownBy { Await.result(result, Inf) }
    }
  }

  private object someStripeException extends StripeException("Some error") with NoStackTrace
}
