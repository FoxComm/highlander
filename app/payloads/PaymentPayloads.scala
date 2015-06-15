package payloads

import utils.Validation

import com.wix.accord.dsl.{validator => createValidator}
import com.wix.accord.{Failure => ValidationFailure, Validator}
import com.wix.accord.dsl._

case class CreditCardPayload(holderName: String, number: String,
                             cvv: String, expYear: Int, expMonth: Int) extends Validation[CreditCardPayload] {
  override def validator = createValidator[CreditCardPayload] { p =>
    p.holderName is notEmpty
    p.number should matchRegex("[0-9]+")
    p.cvv should matchRegex("[0-9]{3,4}")
  }
}

case class PaymentMethodPayload(cardholderName: String, cardNumber: String,  cvv: Int, expiration: String)

case class TokenizedPaymentMethodPayload(gateway: String,
                                         token: String) extends Validation[TokenizedPaymentMethodPayload] {
  override def validator = createValidator[TokenizedPaymentMethodPayload] { p =>
    p.gateway is notEmpty
    p.token is notEmpty
  }
}

