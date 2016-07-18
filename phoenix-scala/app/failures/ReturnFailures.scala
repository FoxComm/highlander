package failures

import utils.friendlyClassName

object ReturnFailures {

  case class EmptyReturn(refNum: String) extends Failure {
    override def description = s"rma with referenceNumber=$refNum has no line items"
  }

  object ReturnPaymentNotFoundFailure {
    def apply[M](m: M): NotFoundFailure400 =
      NotFoundFailure400(s"${friendlyClassName(m)} payment not found")
  }
}
