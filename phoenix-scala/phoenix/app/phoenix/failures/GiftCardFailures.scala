package failures

import models.payment.giftcard.GiftCard

object GiftCardFailures {

  case class GiftCardMustBeCart(code: String) extends Failure {
    override def description = s"giftCart with code=$code is not in cart state"
  }

  case class GiftCardMustNotBeCart(code: String) extends Failure {
    override def description = s"giftCart with code=$code must not be in cart state"
  }

  case class GiftCardConvertFailure(gc: GiftCard) extends Failure {
    override def description = s"cannot convert a gift card with state '${gc.state}'"
  }

  case class GiftCardPaymentAlreadyAdded(refNum: String, code: String) extends Failure {
    override def description =
      s"Gift Card with code=$code already added as payment method to order with refNum=$refNum"
  }

  case class GiftCardPaymentNotFound(refNum: String, code: String) extends Failure {
    override def description =
      s"Gift Card with code=$code is not added as payment method to order with refNum=$refNum"
  }

  case class GiftCardAuthAdjustmentNotFound(orderPaymentId: Int) extends Failure {
    override def description =
      s"Cannot capture a Gift Card using order payment $orderPaymentId because no adjustment in auth"
  }

  case class GiftCardNotEnoughBalance(gc: GiftCard, requestedAmount: Int) extends Failure {
    override def description =
      s"Gift Card with code=${gc.code} has availableBalance=${gc.availableBalance} less than requestedAmount=$requestedAmount"
  }

  case class GiftCardIsInactive(gc: GiftCard) extends Failure {
    override def description = s"Gift Card with id=${gc.id} is inactive"
  }

  case object CreditCardMustHaveAddress extends Failure {
    override def description = "cannot create Credit Card without an address"
  }
}
