package payloads

import cats.data.ValidatedNel
import cats.implicits._
import failures.Failure
import org.json4s.JsonAST.JObject
import payloads.AddressPayloads.CreateAddressPayload
import utils.Money.Currency
import utils._
import utils.aliases._

object PaymentPayloads {

  case class CreateCreditCardFromTokenPayload(token: String,
                                              lastFour: String,
                                              expYear: Int,
                                              expMonth: Int,
                                              brand: String,
                                              holderName: String,
                                              billingAddress: CreateAddressPayload,
                                              addressIsNew: Boolean) {
    def validate: ValidatedNel[Failure, CreateCreditCardFromTokenPayload] = {
      import Validation._

      val tokenNotEmpty      = notEmpty(token, "token")
      val validYear          = withinTwentyYears(expYear, "expiration")
      val validMonth         = isMonth(expMonth, "expiration")
      val expDateInFuture    = notExpired(expYear, expMonth, "credit card is expired")
      val notEmptyBrand      = notEmpty(brand, "brand")
      val notEmptyHolderName = notEmpty(holderName, "holder name")
      val fieldsValid = tokenNotEmpty |@| validYear |@| validMonth |@| expDateInFuture |@| notEmptyBrand |@|
          notEmptyHolderName
      (fieldsValid |@| billingAddress.validate).map { case _ ⇒ this }
    }

  }

  @deprecated(message = "Use `CreateCreditCardFromTokenPayload` instead",
              "Until we are PCI compliant")
  case class CreateCreditCardFromSourcePayload(holderName: String,
                                               cardNumber: String,
                                               cvv: String,
                                               expYear: Int,
                                               expMonth: Int,
                                               address: Option[CreateAddressPayload] = None,
                                               addressId: Option[Int] = None,
                                               isDefault: Boolean = false,
                                               isShipping: Boolean = false) {

    def validate: ValidatedNel[Failure, CreateCreditCardFromSourcePayload] = {
      import Validation._

      def someAddress: ValidatedNel[Failure, _] =
        validExpr(address.isDefined || addressId.isDefined, "address or addressId")

      (notEmpty(holderName, "holderName") |@| matches(cardNumber, "[0-9]+", "number") |@| matches(
              cvv,
              "[0-9]{3,4}",
              "cvv") |@| withinTwentyYears(expYear, "expiration") |@| isMonth(expMonth,
                                                                              "expiration") |@| notExpired(
              expYear,
              expMonth,
              "credit card is expired") |@| someAddress).map { case _ ⇒ this }
    }

    def lastFour: String = this.cardNumber.takeRight(4)
  }

  case class PaymentMethodPayload(cardholderName: String,
                                  cardNumber: String,
                                  cvv: Int,
                                  expiration: String)

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
              isMonth(_, "expiration")) |@| expired).map {
        case _ ⇒ this
      }
    }
  }

  case class GiftCardPayment(code: String, amount: Option[Int] = None)

  case class StoreCreditPayment(amount: Int)

  case class CreditCardPayment(creditCardId: Int)

  case class CreateManualStoreCredit(amount: Int,
                                     currency: Currency = Currency.USD,
                                     reasonId: Int,
                                     subReasonId: Option[Int] = None,
                                     subTypeId: Option[Int] = None,
                                     scope: Option[String] = None)

  case class CreateExtensionStoreCredit(amount: Int,
                                        currency: Currency = Currency.USD,
                                        subTypeId: Option[Int] = None,
                                        metadata: Json = JObject(),
                                        scope: Option[String] = None)
}
