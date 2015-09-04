package payloads

import services.Failure
import utils.{Checks, ValidationNew}
import cats.data.ValidatedNel
import cats.implicits._
import utils.Litterbox._

final case class CreateCreditCard(holderName: String, number: String, cvv: String, expYear: Int,
  expMonth: Int, address: Option[CreateAddressPayload] = None, addressId: Option[Int] = None,
  isDefault: Boolean = false)
  extends ValidationNew[CreateCreditCard] {

  def validate: ValidatedNel[Failure, CreateCreditCard] = {
    def someAddress: ValidatedNel[Failure, _] =
      Checks.validExpr((address.isDefined || addressId.isDefined), "address or addressId must be defined")

    (Checks.notEmpty(holderName, "holderName")
      |@| Checks.matches(number, "[0-9]+", "number")
      |@| Checks.matches(cvv, "[0-9]{3,4}", "cvv")
      |@| Checks.notExpired(expYear, expMonth, "credit card is expired")
      |@| Checks.withinNumberOfYears(expYear, expMonth, 20, "credit card expiration is too far in the future")
      |@| Checks.matches(number, "[0-9]+", "number")
      |@| someAddress
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
