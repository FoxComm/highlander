package failures

import models.returns.ReturnReason
import utils.friendlyClassName

object ReturnFailures {

  case class NoReturnsFoundForOrder(refNum: String) extends Failure {
    override def description = s"no return for order $refNum was found"
  }

  case class EmptyReturn(refNum: String) extends Failure {
    override def description = s"return with referenceNumber=$refNum has no line items"
  }

  object ReturnPaymentNotFoundFailure {
    def apply[M](m: M): NotFoundFailure400 =
      NotFoundFailure400(s"${friendlyClassName(m)} payment not found")
  }

  object ReturnReasonNotFoundFailure {
    def apply(id: ReturnReason#Id): NotFoundFailure400 =
      NotFoundFailure400(s"Return reason $id not found")
  }
}
