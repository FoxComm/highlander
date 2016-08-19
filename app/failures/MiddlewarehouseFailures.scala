package failures

import com.stripe.exception.StripeException

object MiddlewarehouseFailures {

  case class UnableToReserveLineItems(error: Throwable) extends Failure {
    override def description = s"Unable to reserve line items: $error"
  }
}
