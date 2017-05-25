package phoenix.failures

import core.failures.{Failure, NotFoundFailure404}

object ShippingMethodFailures {

  case class ShippingMethodNotFoundInOrder(orderRef: String) extends Failure {
    override def description = s"Shipping method is not present in order $orderRef"
  }

  case class ShippingMethodNotFoundByName(name: String) extends Failure {
    override def description = s"Shipping method with name: '$name' cannot be found"
  }

  case class ShippingMethodIsNotActive(id: Int) extends Failure {
    override def description = s"Shipping method $id can't be added because it's not active"
  }

  case class ShippingMethodNotApplicableToCart(id: Int, refNum: String) extends Failure {
    override def description = s"Shipping method $id is not applicable to cart $refNum"
  }

  object NoDefaultShippingMethod {
    def apply(): Failure = NotFoundFailure404(s"No default shipping method found")
  }
}
