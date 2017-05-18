package utils.apis

import cats.implicits._
import scala.concurrent.Await
import scala.concurrent.duration.Duration.Inf
import scala.util.control.NoStackTrace

import com.stripe.exception.StripeException
import failures.StripeFailures.StripeFailure
import testutils.TestBase

class StripeApiErrorHandlingTest extends TestBase {

  import scala.concurrent.ExecutionContext.Implicits.global // for Monad[Future]

  "Stripe API" - {
    "catches StripeException and returns a Result.failure" in {
      def boom = throw someStripeException

      // FIXME: how to get rid of the explicit StateT#runEmptyA operation below? Why `Await.result` and not `.futureValue` (`.gimmeFailures`) with tuned timeouts? @michalrus
      val result = Await.result(new StripeWrapper().inBlockingPool(boom).runEmptyA.value, Inf)
      leftValue(result).head must === (StripeFailure(someStripeException))
    }

    "does not catch other exceptions" in {
      // Must be A <: AnyRef
      case class IntWrapper(value: Int)
      lazy val oops = IntWrapper(42 / 0)

      /** Scalatest’s futureValue wraps the exception, so we can’t use it here. */
      an[ArithmeticException] must be thrownBy {
        Await.result(new StripeWrapper().inBlockingPool(oops).runEmptyA.value, Inf)
      }
    }
  }

  private object someStripeException
      extends StripeException("Some error", "X_REQUEST_ID: 1", 400)
      with NoStackTrace
}
