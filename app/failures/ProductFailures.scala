package failures

object ProductFailures {

  final case class SkuNotFoundForContext(code: String, productContext: String) extends Failure {
    override def description = s"Sku $code with product context $productContext cannot be found"
  }

  final case class ProductNotFoundForContext(productId: Int, productContextId: Int) extends Failure {
    override def description = s"Product with id=$productId with product context $productContextId cannot be found"
  }

  final case class ProductContextNotFound(name: String) extends Failure {
    override def description = s"Product Context with name $name cannot be found"
  }

  final case class ProductShadowHasInvalidAttribute(key: String, value: String) extends Failure {
    override def description = s"Product shadow has an invalid attribute $key with value $value"
  }

  final case class ProductShadowAttributeNotAString(key: String) extends Failure {
    override def description = s"Product shadow attribute $key must be a string"
  }

  case object ProductAttributesAreEmpty extends Failure {
    override def description = s"Product attributes are empty"
  }

  case object ProductShadowAttributesAreEmpty extends Failure {
    override def description = s"Product shadow attributes are empty"
  }

  final case class NoVariantForContext(context: String) extends Failure {
    override def description = s"No variant context $context"
  }

}
