package payloads

import cats.data.ValidatedNel
import cats.implicits._
import services.Failure
import utils.Litterbox._
import utils.Validation

final case class CreateCreditCard(holderName: String, number: String, cvv: String, expYear: Int,
  expMonth: Int, address: Option[CreateAddressPayload] = None, isDefault: Boolean = false) {

  def validate: ValidatedNel[Failure, CreateCreditCard] = {
    ( Validation.notEmpty(holderName, "holderName")
      |@| Validation.matches(number, "[0-9]+", "bodySize")
      |@| Validation.matches(cvv, "[0-9]{3,4}", "cvv")
      |@| Validation.between(expYear, 2015, 2050, "Expiration year should be between 2015 and 2050")
      |@| Validation.between(expMonth, 1, 12, "Expiration month should be between 1 and 12")
      ).map { case _ ⇒ this }
  }

  def lastFour: String = this.number.takeRight(4)
}

final case class PaymentMethodPayload(cardholderName: String, cardNumber: String,  cvv: Int, expiration: String)

final case class ToggleDefaultCreditCard(isDefault: Boolean)

final case class EditCreditCard(
  holderName: Option[String] = None,
  expYear:    Option[Int] = None,
  expMonth:   Option[Int] = None,
  address:    Option[String] = None,
  address2:   Option[String] = None,
  state:      Option[String] = None,
  city:       Option[String] = None,
  zip:        Option[String] = None)

final case class GiftCardPayment(code: String, amount: Int)

final case class StoreCreditPayment(amount: Int)

final case class CreditCardPayment(creditCardId: Int)
