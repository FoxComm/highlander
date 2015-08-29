package payloads

import utils.Validation

import com.wix.accord.dsl.{validator => createValidator}
import com.wix.accord.{Failure => ValidationFailure, Validator}
import com.wix.accord.dsl._

final case class CreateCreditCard(holderName: String, number: String, cvv: String, expYear: Int,
  expMonth: Int, address: Option[CreateAddressPayload] = None, isDefault: Boolean = false)
  extends Validation[CreateCreditCard] {

  override def validator = createValidator[CreateCreditCard] { cc =>
    cc.holderName is notEmpty
    cc.number should matchRegex("[0-9]+")
    cc.cvv should matchRegex("[0-9]{3,4}")
    cc.expYear is between(2015, 2050)
    cc.expMonth is between(1, 12)
    // TODO: this is why we'd use implicit validators
    // cc.address.map { a => a is valid }
  }

  def lastFour: String = this.number.takeRight(4)
}

final case class PaymentMethodPayload(cardholderName: String, cardNumber: String,  cvv: Int, expiration: String)

final case class ToggleDefaultCreditCard(isDefault: Boolean)

final case class EditCreditCard(
  cvv:      Option[String] = None,
  expYear:  Option[Int] = None,
  expMonth: Option[Int] = None,
  address:  Option[UpdateAddressPayload] = None)

final case class GiftCardPayment(code: String, amount: Int)

final case class StoreCreditPayment(amount: Int)

final case class CreditCardPayment(creditCardId: Int)
