package utils

import scala.util.control.NoStackTrace

import com.stripe.exception.StripeException
import services.StripeRuntimeException
import util.TestBase

class WiredStripeApiTest extends TestBase {
  "Wired Stripe API" - {
    "catches StripeException and returns a Result.failure" in {
      val api    = new WiredStripeApi
      val result = api.inBlockingPool("abc")(_ ⇒ throw someStripeException)

      leftValue(result.futureValue).head must === (StripeRuntimeException(someStripeException))
    }
  }

  private object someStripeException extends StripeException("Some error") with NoStackTrace
}
