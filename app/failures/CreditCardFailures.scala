package failures

import com.stripe.exception.StripeException
import models.payment.creditcard.CreditCard

object CreditCardFailures {

  case class StripeFailure(exception: StripeException) extends Failure {
    override def description = exception.getMessage
  }

  case object InvalidCvc extends Failure {
    override def description = "failed CVC check"
  }

  case object IncorrectCvc extends Failure {
    override def description = "The credit card's cvc is incorrect"
  }

  case object MonthExpirationInvalid extends Failure {
    override def description = "The credit card's month expiration is invalid"
  }

  case object YearExpirationInvalid extends Failure {
    override def description = "The credit card's year expiration is invalid"
  }

  case object IncorrectNumber extends Failure {
    override def description = "The credit card's number is incorrect"
  }

  case object InvalidNumber extends Failure {
    override def description = "The card number is not a valid credit card number"
  }

  case object ExpiredCard extends Failure {
    override def description = "The credit card is expired"
  }

  case object IncorrectZip extends Failure {
    override def description = "The zip code failed verification"
  }

  case object CardDeclined extends Failure {
    override def description = "The credit card was declined"
  }

  case object Missing extends Failure {
    override def description = "Could not find a credit card for the customer"
  }

  case object ProcessingError extends Failure {
    override def description = "There was an error processing the credit card request"
  }

  case class CannotUseInactiveCreditCard(cc: CreditCard) extends Failure {
    override def description = s"creditCard with id=${cc.id} is inactive"
  }

  case class CustomerHasNoCreditCard(customerId: Int) extends Failure {
    override def description = s"No credit card found for customer with id=$customerId"
  }

  case object CustomerHasDefaultCreditCard extends Failure {
    override def description = "customer already has default credit card"
  }
}
