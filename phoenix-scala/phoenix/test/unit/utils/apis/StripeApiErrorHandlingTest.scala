package phoenix.utils.apis

import cats.implicits._
import com.stripe.exception.StripeException
import phoenix.failures.StripeFailures.{StripeFailure, StripeProcessingFailure}
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration.Inf
import scala.concurrent.duration._
import scala.util.control.NoStackTrace
import testutils.TestBase

class StripeApiErrorHandlingTest extends TestBase { // for Monad[Future]

  "Stripe API" - {
    val timeout = 250.millis

    "catches StripeException and returns a Result.failure" in {
      def boom = throw someStripeException

      // FIXME: how to get rid of the explicit StateT#runEmptyA operation below? Why `Await.result` and not `.futureValue` (`.gimmeFailures`) with tuned timeouts? @michalrus
      val result =
        Await.result(new StripeWrapper(timeout, timeout / 3, timeout).inBlockingPool(boom).runEmptyA.value,
                     Inf)
      result.leftVal.head must === (StripeFailure(someStripeException))
    }

    "does not catch other exceptions" in {
      // Must be A <: AnyRef
      case class IntWrapper(value: Int)
      lazy val oops = IntWrapper(42 / 0)

      /** Scalatest’s futureValue wraps the exception, so we can’t use it here. */
      an[ArithmeticException] must be thrownBy {
        Await.result(new StripeWrapper(timeout, timeout / 3, timeout).inBlockingPool(oops).runEmptyA.value,
                     Inf)
      }
    }

    "catches timeouts in stripe API" in {
      def boom = { Thread.sleep(300); throw someStripeException }

      val result =
        Await.result(new StripeWrapper(timeout, timeout / 3, timeout).inBlockingPool(boom).runEmptyA.value,
                     Inf)
      result.leftVal.head must === (StripeProcessingFailure("Request to Stripe timed out"))
    }
  }

  private object someStripeException
      extends StripeException("Some error", "X_REQUEST_ID: 1", 400)
      with NoStackTrace
}
