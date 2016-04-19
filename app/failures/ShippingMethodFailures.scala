package failures

object ShippingMethodFailures {

  case class ShippingMethodIsNotFound(shippingMethodId: Int) extends Failure {
    override def description = s"Shipping method $shippingMethodId can't be found"
  }

  case class ShippingMethodIsNotActive(shippingMethodId: Int) extends Failure {
    override def description = s"Shipping method $shippingMethodId can't be added because it's not active"
  }

  case class ShippingMethodNotApplicableToOrder(shippingMethodId: Int, referenceNumber: String) extends Failure {
    override def description = s"Shipping method $shippingMethodId is not applicable to order $referenceNumber"
  }
}
