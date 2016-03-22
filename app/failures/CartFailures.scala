package failures

object CartFailures {

  final case class OrderMustBeCart(refNum: String) extends Failure {
    override def description = s"order with referenceNumber=$refNum is not in cart state"
  }

  final case class CustomerHasCart(id: Int) extends Failure {
    override def description = s"customer with id=$id already has an active cart"
  }

  final case class CustomerHasNoActiveOrder(customerId: Int) extends Failure {
    override def description = s"customer with id=$customerId has no active order"
  }

  final case class EmptyCart(refNum: String) extends Failure {
    override def description = s"order with referenceNumber=$refNum is an empty cart"
  }

  final case class NoShipAddress(refNum: String) extends Failure {
    override def description = s"order with referenceNumber=$refNum has no shipping address"
  }

  final case class NoShipMethod(refNum: String) extends Failure {
    override def description = s"order with referenceNumber=$refNum has no shipping method"
  }

  final case class InvalidShippingMethod(refNum: String) extends Failure {
    override def description = s"order with referenceNumber=$refNum has invalid shipping method"
  }

  final case class InsufficientFunds(refNum: String) extends Failure {
    override def description = s"order with referenceNumber=$refNum has insufficient funds"
  }
}
