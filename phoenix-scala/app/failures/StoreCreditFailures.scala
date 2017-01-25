package failures

import models.payment.storecredit.StoreCredit

object StoreCreditFailures {

  case class StoreCreditConvertFailure(state: StoreCredit.State) extends Failure {
    override def description = s"cannot convert a store credit with state '$state'"
  }

  case class StoreCreditIsInactive(sc: StoreCredit) extends Failure {
    override def description = s"storeCredit with id=${sc.id} is inactive"
  }

  case class CustomerHasInsufficientStoreCredit(id: Int, has: Int, want: Int) extends Failure {
    override def description =
      s"customer with id=$id has storeCredit=$has less than requestedAmount=$want"
  }
}
