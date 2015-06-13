package payloads

import utils.Validation

import com.wix.accord.dsl.{validator => createValidator}
import com.wix.accord.{Failure => ValidationFailure, Validator}
import com.wix.accord.dsl._

case class CreditCardPayload(holderName: String, number: String,  cvv: String, expYear: Int, expMonth: Int) extends Validation {
  override def validator[T] = {
    createValidator[CreditCardPayload] { p =>
      p.holderName is notEmpty
      p.number should matchRegex("[0-9]+")
      p.cvv should matchRegex("[0-9]{3,4}")
    }
  }.asInstanceOf[Validator[T]] // TODO: fix me!
}

case class PaymentMethodPayload(cardholderName: String, cardNumber: String,  cvv: Int, expiration: String)

case class TokenizedPaymentMethodPayload(paymentGateway: String,
                                         paymentGatewayToken: String) extends Validation[TokenizedPaymentMethodPayload] {
  override def validator = createValidator[TokenizedPaymentMethodPayload] { p =>
    p.paymentGateway is notEmpty
    p.paymentGatewayToken is notEmpty
  }
}

