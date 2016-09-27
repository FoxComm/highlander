package payloads

import cats.data.ValidatedNel
import cats.implicits._
import failures.Failure
import org.json4s.JsonAST.JObject
import payloads.AddressPayloads.CreateAddressPayload
import utils.Money.Currency
import utils.Validation._
import utils._
import utils.aliases._

object PaymentPayloads {

  case class CreateCcAddressPayload(address: CreateAddressPayload, isNew: Boolean = false)
  case class UpdateCcAddressPayload(address: CreateAddressPayload, id: Option[Int])

  trait CreateCreditCardPayloadsBase[A] extends Validation[A] { self: A ⇒
    def expYear: Int
    def expMonth: Int
    def holderName: String
    def lastFour: String
    def billingAddress: CreateCcAddressPayload

    private val yearValid         = withinTwentyYears(expYear, "expiration year")
    private val monthValid        = isMonth(expMonth, "expiration month")
    private val expDateInFuture   = notExpired(expYear, expMonth, "credit card is expired")
    private val holderNamePresent = notEmpty(holderName, "holder name")
    private val lastFourValid     = matches(lastFour, "[0-9]{4}", "last four")
    private val sharedValidations = yearValid |@| monthValid |@| expDateInFuture |@| holderNamePresent |@| lastFourValid

    def customValidations: ValidatedNel[Failure, Unit]

    def validate: ValidatedNel[Failure, A] = {
      (sharedValidations |@| customValidations |@| billingAddress.address.validate).map {
        case _ ⇒ this
      }
    }
  }

  case class CreateCreditCardFromTokenPayload(token: String,
                                              lastFour: String,
                                              expYear: Int,
                                              expMonth: Int,
                                              brand: String,
                                              holderName: String,
                                              billingAddress: CreateCcAddressPayload)
      extends CreateCreditCardPayloadsBase[CreateCreditCardFromTokenPayload] {

    def customValidations: ValidatedNel[Failure, Unit] = {
      val tokenNotEmpty = notEmpty(token, "token")
      val notEmptyBrand = notEmpty(brand, "brand")
      (tokenNotEmpty |@| notEmptyBrand).map { case _ ⇒ {} }
    }

  }

  // !!! Make sure sensitive data is not logged when enabling this back !!!
  @deprecated(message = "Use `CreateCreditCardFromTokenPayload` instead",
              "Until we are PCI compliant")
  case class CreateCreditCardFromSourcePayload(holderName: String,
                                               cardNumber: String,
                                               cvv: String,
                                               expYear: Int,
                                               expMonth: Int,
                                               billingAddress: CreateCcAddressPayload,
                                               isDefault: Boolean = false)
      extends CreateCreditCardPayloadsBase[CreateCreditCardFromSourcePayload] {

    val customValidations: ValidatedNel[Failure, Unit] = {
      val cardNumberValid = matches(cardNumber, "[0-9]+", "number")
      val cvvValid        = matches(cvv, "[0-9]{3,4}", "cvv")
      (cardNumberValid |@| cvvValid).map { case _ ⇒ {} }
    }

    def lastFour: String = this.cardNumber.takeRight(4)
  }

  case class PaymentMethodPayload(cardholderName: String,
                                  cardNumber: String,
                                  cvv: Int,
                                  expiration: String)

  case class ToggleDefaultCreditCard(isDefault: Boolean)

  case class EditCreditCardPayload(holderName: Option[String] = None,
                                   expYear: Option[Int] = None,
                                   expMonth: Option[Int] = None,
                                   address: Option[UpdateCcAddressPayload] = None) {

    def validate: ValidatedNel[Failure, EditCreditCardPayload] = {

      val expired: ValidatedNel[Failure, Unit] = (expYear |@| expMonth).tupled.fold(ok) {
        case (y, m) ⇒ notExpired(y, m, "credit card is expired")
      }

      val holderNamePresent = holderName.fold(ok)(notEmpty(_, "holder name"))
      val expYearValid      = expYear.fold(ok)(withinTwentyYears(_, "expiration year"))
      val expMonthOk        = expMonth.fold(ok)(isMonth(_, "expiration"))

      val atLeastOneNewValueDefined = {
        val allOpts = Seq(holderName, expYear, expMonth, address)
        notEmpty(allOpts.find(_.isDefined).flatten, "At least one of new values")
      }

      (atLeastOneNewValueDefined |@| holderNamePresent |@| expYearValid |@| expMonthOk |@| expired).map {
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
                                     subTypeId: Option[Int] = None)

  case class CreateExtensionStoreCredit(amount: Int,
                                        currency: Currency = Currency.USD,
                                        subTypeId: Option[Int] = None,
                                        metadata: Json = JObject())
}
