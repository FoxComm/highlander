package utils

import scala.util.control.NoStackTrace

import com.stripe.exception.StripeException
import services.StripeRuntimeException
import util.TestBase

class WiredStripeApiTest extends TestBase {
  "Wired Stripe API" - {
    "catches StripeException and returns a Result.failure" in {
      val api    = new WiredStripeApi
      val result = api.inBlockingPool("abc")(_ ⇒ throw someStripException)

      leftValue(result.futureValue).head must matchPattern  {
        case StripeRuntimeException(someStripException) ⇒
      }
    }
  }

  private object someStripException extends StripeException("Some error") with NoStackTrace
}
