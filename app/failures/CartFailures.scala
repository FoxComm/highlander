package failures

object CartFailures {

  case class OrderMustBeCart(refNum: String) extends Failure {
    override def description = s"order with referenceNumber=$refNum is not in cart state"
  }

  case class CustomerHasCart(id: Int) extends Failure {
    override def description = s"customer with id=$id already has an active cart"
  }

  case class CustomerHasNoActiveOrder(customerId: Int) extends Failure {
    override def description = s"customer with id=$customerId has no active order"
  }

  case class EmptyCart(refNum: String) extends Failure {
    override def description = s"order with referenceNumber=$refNum is an empty cart"
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
}
