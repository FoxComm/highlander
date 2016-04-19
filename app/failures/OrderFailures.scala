package failures

import utils.friendlyClassName

object OrderFailures {

  case class SkuNotFoundInOrder(sku: String, refNum: String) extends Failure {
    override def description = s"line item with sku=$sku not found in order with referenceNumber=$refNum"
  }

  object OrderPaymentNotFoundFailure {
    def apply[M](m: M): NotFoundFailure400 = NotFoundFailure400(s"${friendlyClassName(m)} payment not found")
  }

  case class OrderUpdateFailure(referenceNumber: String, reason: String) extends Failure {
    override def description = reason
  }

  case object EmptyRefNumFailure extends Failure {
    override def description = "Please provide an order reference number"
  }

}
