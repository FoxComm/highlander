package phoenix.failures

import com.stripe.exception.StripeException
import core.failures.Failure

object StripeFailures {

  case object UnableToReadStripeApiKey extends Failure {
    override def description = "Could not read Stripe API key"
  }

  case class StripeFailure(exception: StripeException) extends Failure {
    override def description = exception.getMessage
  }

  case class CardNotFoundForNewCustomer(stripeCustomerId: String) extends Failure {
    override def description: String =
      s"Stripe customer $stripeCustomerId expected to have a card, found none"
  }

  case class StripeProcessingFailure(message: String) extends Failure {
    override def description = s"Failed to process stripe request with error: ${message}"
  }
}
