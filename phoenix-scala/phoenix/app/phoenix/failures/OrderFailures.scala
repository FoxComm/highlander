package failures

import utils.friendlyClassName

object OrderFailures {

  case class SkuNotFoundInOrder(sku: String, refNum: String) extends Failure {
    override def description =
      s"line item with sku=$sku not found in order with referenceNumber=$refNum"
  }

  object OrderPaymentNotFoundFailure {
    def apply[M](m: M): NotFoundFailure400 =
      NotFoundFailure400(s"${friendlyClassName(m)} payment not found")
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
    override def description = s"Order line item with referenceNumber=${refNum} not found"
  }
}
