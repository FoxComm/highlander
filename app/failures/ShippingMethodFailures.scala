package failures

object ShippingMethodFailures {

  case class ShippingMethodNotFoundInOrder(orderRef: String) extends Failure {
    override def description = s"Shipping method is not present in order $orderRef"
  }

  case class ShippingMethodNotFoundByName(name: String) extends Failure {
    override def description = s"Shipping method with name: '$name' cannot be found"
  }

  case class ShippingMethodIsNotActive(shippingMethodId: Int) extends Failure {
    override def description =
      s"Shipping method $shippingMethodId can't be added because it's not active"
  }

  case class ShippingMethodNotApplicableToCart(shippingMethodId: Int, referenceNumber: String)
      extends Failure {
    override def description =
      s"Shipping method $shippingMethodId is not applicable to cart $referenceNumber"
  }
}
