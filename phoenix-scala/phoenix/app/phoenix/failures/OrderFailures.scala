package phoenix.failures

import core.failures.{Failure, NotFoundFailure400}
import core.utils.friendlyClassName

object OrderFailures {

  case class SkuNotFoundInOrder(sku: String, refNum: String) extends Failure {
    override def description =
      s"line item with sku=$sku not found in order with referenceNumber=$refNum"
  }

  object OrderPaymentNotFoundFailure {
    def apply[M](m: M): NotFoundFailure400 =
      NotFoundFailure400(s"${friendlyClassName(m)} payment not found")
  }

  case object OnlyOneExternalPaymentIsAllowed extends Failure {
    def description: String = "Only one payment method is allowed (credit card or apple pay)!"
  }

  case object NoExternalPaymentsIsProvided extends Failure {
    def description: String = "No external payments is provided!"
  }

  case object ApplePayIsNotProvided extends Failure {
    def description: String = "No Apple Pay payment is provided!"
  }

  case object CreditCardIsNotProvided extends Failure {
    def description: String = "No credit card is provided!"
  }

  case class OrderUpdateFailure(referenceNumber: String, reason: String) extends Failure {
    override def description = reason
  }

  case object EmptyRefNumFailure extends Failure {
    override def description = "Please provide an order reference number"
  }

  case object OrderAlreadyHasCoupon extends Failure {
    override def description = "Order already has a coupon attached. Remove coupon first."
  }

  case class OrderLineItemNotFound(refNum: String) extends Failure {
    override def description = s"Order line item with referenceNumber=$refNum not found"
  }
}
