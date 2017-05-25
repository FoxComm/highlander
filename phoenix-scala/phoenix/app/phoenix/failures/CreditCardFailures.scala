package phoenix.failures

import core.failures.{Failure, NotFoundFailure404}
import phoenix.models.payment.creditcard.CreditCard

object CreditCardFailures {

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

  case class CustomerHasNoCreditCard(accountId: Int) extends Failure {
    override def description = s"No credit card found for customer with id=$accountId"
  }

  case object CustomerHasDefaultCreditCard extends Failure {
    override def description = "customer already has default credit card"
  }

  object NoDefaultCreditCardForCustomer {
    def apply(): Failure = NotFoundFailure404("No default credit card defined")
  }
}
