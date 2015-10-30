package payloads

import cats.data.ValidatedNel
import cats.data.Validated.valid
import cats.implicits._
import services.Failure
import utils.Litterbox._
import utils.Money.Currency
import utils._

final case class CreateCreditCard(holderName: String, number: String, cvv: String, expYear: Int,
  expMonth: Int, address: Option[CreateAddressPayload] = None, addressId: Option[Int] = None,
  isDefault: Boolean = false) {

  def validate: ValidatedNel[Failure, CreateCreditCard] = {
    import Validation._

    def someAddress: ValidatedNel[Failure, _] =
      validExpr(address.isDefined || addressId.isDefined, "address or addressId")

    ( notEmpty(holderName, "holderName")
      |@| matches(number, "[0-9]+", "number")
      |@| matches(cvv, "[0-9]{3,4}", "cvv")
      |@| withinTwentyYears(expYear, "expiration")
      |@| isMonth(expMonth, "expiration")
      |@| notExpired(expYear, expMonth, "credit card is expired")
      |@| someAddress
      ).map { case _ ⇒ this }
  }

  def lastFour: String = this.number.takeRight(4)
}

final case class PaymentMethodPayload(cardholderName: String, cardNumber: String,  cvv: Int, expiration: String)

final case class ToggleDefaultCreditCard(isDefault: Boolean)

final case class EditCreditCard(holderName: Option[String] = None, expYear: Option[Int] = None,
  expMonth: Option[Int] = None, addressId: Option[Int] = None, address: Option[CreateAddressPayload] = None) {

  def validate: ValidatedNel[Failure, EditCreditCard] = {
    import Validation._

    val expired: ValidatedNel[Failure, Unit] =
      (expYear |@| expMonth).tupled.fold(ok) { case (y, m) ⇒ notExpired(y, m, "credit card is expired") }

    ( holderName.fold(ok)(notEmpty(_, "holderName"))
      |@| expYear.fold(ok)(withinTwentyYears(_, "expiration"))
      |@| expMonth.fold(ok)(isMonth(_, "expiration"))
      |@| expired
    ).map { case _ ⇒ this }
  }
}

final case class GiftCardPayment(code: String, amount: Int)

final case class StoreCreditPayment(amount: Int)

final case class CreditCardPayment(creditCardId: Int)

final case class CreateManualStoreCredit(amount: Int, currency: Currency = Currency.USD,
  reasonId: Int, subReasonId: Option[Int] = None, subTypeId: Option[Int] = None)
