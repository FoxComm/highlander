package failures

object ProductFailures {

  final case class SkuNotFound(code: String) extends Failure {
    override def description = s"Sku $code not found"
  }

  final case class ProductNotFoundAtCommit(id: Int, commit: Int) extends Failure {
    override def description = s"Product $id not with at commit $commit"
  }

  final case class SkuShadowNotFoundInPayload(code: String) extends Failure {
    override def description = s"Sku shadow with code $code not found in payload"
  }

  final case class SkuNotFoundForContext(code: String, productContext: String) extends Failure {
    override def description = s"Sku $code with product context $productContext cannot be found"
  }

  final case class ProductNotFoundForContext(productId: Int, productContextId: Int) extends Failure {
    override def description = s"Product with id=$productId with product context $productContextId cannot be found"
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

  final case class ProductFormNotFound(id: Int) extends Failure {
    override def description = s"Product Form with id $id cannot be found"
  }

  final case class NoVariantForContext(context: String) extends Failure {
    override def description = s"No variant context $context"
  }

}
