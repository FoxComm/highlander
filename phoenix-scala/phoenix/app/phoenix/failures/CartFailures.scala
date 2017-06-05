package phoenix.failures

import core.failures.Failure

object CartFailures {

  case class OrderAlreadyPlaced(refNum: String) extends Failure {
    override def description = s"order with referenceNumber=$refNum is not a cart"
  }

  case class CustomerHasCart(accountId: Int) extends Failure {
    override def description = s"customer with id=$accountId already has an active cart"
  }

  case class CustomerHasNoCart(accountId: Int) extends Failure {
    override def description = s"customer with id=$accountId has no active cart"
  }

  case class EmptyCart(refNum: String) extends Failure {
    override def description = s"order with referenceNumber=$refNum is an empty cart"
  }

  case class NoCartFound(customerId: Int) extends Failure {
    override def description = s"no cart was found for a customer with id=$customerId"
  }

  case class NoShipAddress(refNum: String) extends Failure {
    override def description = s"order with referenceNumber=$refNum has no shipping address"
  }

  case class NoShipMethod(refNum: String) extends Failure {
    override def description = s"order with referenceNumber=$refNum has no shipping method"
  }

  case class InvalidShippingMethod(refNum: String) extends Failure {
    override def description = s"order with referenceNumber=$refNum has invalid shipping method"
  }

  case class InsufficientFunds(refNum: String) extends Failure {
    override def description = s"order with referenceNumber=$refNum has insufficient funds"
  }

  case class SkuWithNoProductAdded(refNum: String, code: String) extends Failure {
    override def description =
      s"item could not be added to cart $refNum. SKU $code does not have an " +
        s"associated product"
  }
}
