package failures

import utils.friendlyClassName

object RmaFailures {

  case class EmptyRma(refNum: String) extends Failure {
    override def description = s"rma with referenceNumber=$refNum has no line items"
  }


  object RmaPaymentNotFoundFailure {
    def apply[M](m: M): NotFoundFailure400 = NotFoundFailure400(s"${friendlyClassName(m)} payment not found")
  }

}
