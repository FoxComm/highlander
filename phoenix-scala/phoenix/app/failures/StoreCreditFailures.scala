package failures

import models.payment.storecredit.StoreCredit

object StoreCreditFailures {

  case class StoreCreditConvertFailure(sc: StoreCredit) extends Failure {
    override def description = s"cannot convert a store credit with state '${sc.state}'"
  }

  case class StoreCreditIsInactive(sc: StoreCredit) extends Failure {
    override def description = s"storeCredit with id=${sc.id} is inactive"
  }

  case class CustomerHasInsufficientStoreCredit(id: Int, has: Int, want: Int) extends Failure {
    override def description =
      s"customer with id=$id has storeCredit=$has less than requestedAmount=$want"
  }

  case class StoreCreditAuthAdjustmentNotFound(orderPaymentId: Int) extends Failure {
    override def description =
      s"Cannot capture a Store Credit using order payment $orderPaymentId because no adjustment in auth"
  }
}
