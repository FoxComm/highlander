package failures

object CartFailures {

  case class OrderAlreadyPlaced(refNum: String) extends Failure {
    override def description = s"order with referenceNumber=$refNum is not a cart"
  }

  case class CustomerHasCart(accountId: Int) extends Failure {
    override def description = s"customer with id=$accountId already has an active cart"
  }

  case class CustomerHasNoCart(accountId: Int) extends Failure {
    override def description = s"customer with id=$accountId has no active order"
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

  case class SkuWithNoProductAdded(refNum: String, code: String) extends Failure {
    override def description =
      s"item could not be added to cart $refNum. SKU $code does not have an " +
        s"associated product"
  }

  case class InactiveProductInCart(id: Int) extends Failure {
    override def description: String =
      s"Product with id='$id' is no longer available and should be removed from cart"
  }

  case class InactiveSkuInCart(code: String) extends Failure {
    override def description: String =
      s"Product with sku '$code' is no longer available and should be removed from cart"
  }
}
