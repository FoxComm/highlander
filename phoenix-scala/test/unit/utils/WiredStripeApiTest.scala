package utils

import scala.concurrent.Await
import scala.concurrent.duration.Duration.Inf
import scala.util.control.NoStackTrace

import com.stripe.exception.StripeException
import failures.StripeFailures.StripeFailure
import util.TestBase
import utils.apis.WiredStripeApi

class WiredStripeApiTest extends TestBase {
  private val api = new WiredStripeApi

  "Wired Stripe API" - {
    "catches StripeException and returns a Result.failure" in {
      def boom = throw someStripeException

      val result = Await.result(api.inBlockingPool(boom), Inf)
      leftValue(result).head must === (StripeFailure(someStripeException))
    }

    "does not catch other exceptions" in {
      lazy val oops = 42 / 0

      /** Scalatest’s futureValue wraps the exception, so we can’t use it here. */
      an[ArithmeticException] must be thrownBy { Await.result(api.inBlockingPool(oops), Inf) }
    }
  }

  private object someStripeException
      extends StripeException("Some error", "X_REQUEST_ID: 1", 400)
      with NoStackTrace
}
