package payloads

import cats.data.ValidatedNel
import cats.implicits._
import failures.Failure
import org.json4s.JsonAST.{JObject, JValue}
import payloads.AddressPayloads.CreateAddressPayload
import utils.Money.Currency
import utils._

object PaymentPayloads {

  case class CreateCreditCard(holderName: String,
                              cardNumber: String,
                              cvv: String,
                              expYear: Int,
                              expMonth: Int,
                              address: Option[CreateAddressPayload] = None,
                              addressId: Option[Int] = None,
                              isDefault: Boolean = false,
                              isShipping: Boolean = false) {

    def validate: ValidatedNel[Failure, CreateCreditCard] = {
      import Validation._

      def someAddress: ValidatedNel[Failure, _] =
        validExpr(address.isDefined || addressId.isDefined, "address or addressId")

      (notEmpty(holderName, "holderName") |@| matches(cardNumber, "[0-9]+", "number") |@| matches(
              cvv,
              "[0-9]{3,4}",
              "cvv") |@| withinTwentyYears(expYear, "expiration") |@| isMonth(expMonth,
                                                                              "expiration") |@| notExpired(
              expYear, expMonth, "credit card is expired") |@| someAddress).map { case _ ⇒ this }
    }

    def lastFour: String = this.cardNumber.takeRight(4)
  }

  case class PaymentMethodPayload(
      cardholderName: String, cardNumber: String, cvv: Int, expiration: String)

  case class ToggleDefaultCreditCard(isDefault: Boolean)

  case class EditCreditCard(holderName: Option[String] = None,
                            expYear: Option[Int] = None,
                            expMonth: Option[Int] = None,
                            addressId: Option[Int] = None,
                            address: Option[CreateAddressPayload] = None,
                            isShipping: Boolean = false) {

    def validate: ValidatedNel[Failure, EditCreditCard] = {
      import Validation._

      val expired: ValidatedNel[Failure, Unit] = (expYear |@| expMonth).tupled.fold(ok) {
        case (y, m) ⇒ notExpired(y, m, "credit card is expired")
      }

      (holderName.fold(ok)(notEmpty(_, "holderName")) |@| expYear
            .fold(ok)(withinTwentyYears(_, "expiration")) |@| expMonth.fold(ok)(
              isMonth(_, "expiration")) |@| expired).map { case _ ⇒ this }
    }
  }

  case class GiftCardPayment(code: String, amount: Option[Int] = None)

  case class StoreCreditPayment(amount: Int)

  case class CreditCardPayment(creditCardId: Int)

  case class CreateManualStoreCredit(amount: Int,
                                     currency: Currency = Currency.USD,
                                     reasonId: Int,
                                     subReasonId: Option[Int] = None,
                                     subTypeId: Option[Int] = None)

  case class CreateExtensionStoreCredit(amount: Int,
                                        currency: Currency = Currency.USD,
                                        subTypeId: Option[Int] = None,
                                        metadata: JValue = JObject())
}
