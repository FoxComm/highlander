package failures

object ShippingMethodFailures {

  case class ShippingMethodNotFound(orderRef: String) extends Failure {
    override def description = s"Shipping method is not present in order $orderRef"
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
