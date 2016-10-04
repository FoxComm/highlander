package payloads

import cats.data.ValidatedNel
import cats.implicits._
import failures.Failure
import models.traits.CreditCardBase
import models.traits.CreditCardValidations._
import org.json4s.JsonAST.JObject
import payloads.AddressPayloads.CreateAddressPayload
import utils.Money.Currency
import utils.Validation._
import utils._
import utils.aliases._

object PaymentPayloads {

  case class CreateCcAddressPayload(address: CreateAddressPayload, isNew: Boolean = false)
  case class UpdateCcAddressPayload(address: CreateAddressPayload, id: Option[Int])

  trait CreateCreditCardPayloadsBase[A] extends CreditCardBase[A] { self: A ⇒
    def billingAddress: CreateCcAddressPayload

    def customValidations: ValidatedNel[Failure, Unit]

    override def validate: ValidatedNel[Failure, A] =
      (super.validate |@| customValidations |@| billingAddress.address.validate).map {
        case _ ⇒ this
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
      (validCardToken(token) |@| validCardBrand(brand)).map { case _ ⇒ {} }
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

    val customValidations: ValidatedNel[Failure, Unit] =
      (validCardNumber(cardNumber) |@| validCvv(cvv)).map { case _ ⇒ {} }

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
        case (year, month) ⇒ validExpDate(year, month)
      }

      val holderNamePresent = holderName.fold(ok)(validHolderName)
      val expYearValid      = expYear.fold(ok)(validExpMonth)
      val expMonthOk        = expMonth.fold(ok)(validExpYear)

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
