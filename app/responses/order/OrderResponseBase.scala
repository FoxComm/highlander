package responses.order

import models.order.Order
import models.payment.creditcard.CreditCardCharge

trait OrderResponseBase {
  def referenceNumber: String
  def orderState: Order.State
  def shippingState: Option[Order.State]
  def paymentState: CreditCardCharge.State
}
