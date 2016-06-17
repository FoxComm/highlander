package failures

import com.stripe.exception.StripeException

object StripeFailures {

  case object UnableToReadStripeApiKey extends Failure {
    override def description = "Could not read Stripe API key"
  }

  case class StripeFailure(exception: StripeException) extends Failure {
    override def description = exception.getMessage
  }
}
