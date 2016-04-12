package failures

object ProductFailures {

  object SkuNotFound { 
    def apply(code: String) = NotFoundFailure404(s"Sku $code not found")
  }

  object SkuWithShadowNotFound { 
    def apply(shadowId: Int) = NotFoundFailure404(s"Sku with shadow id $shadowId not found")
  }

  final case class ProductNotFoundAtCommit(id: Int, commit: Int) extends Failure {
    override def description = s"Product $id not with at commit $commit"
  }

  object SkuShadowNotFoundInPayload { 
    def apply(code: String)  = NotFoundFailure404(s"Sku shadow with code $code not found in payload")
  }

  object SkuNotFoundForContext { def apply(code: String, productContext: String) =
      NotFoundFailure404(s"Sku $code with product context $productContext cannot be found")
  }

  object ProductNotFoundForContext { 
    def apply(productId: Int, productContextId: Int) =
      NotFoundFailure404(s"Product with id=$productId with product context $productContextId cannot be found")
  }

  final case class ProductShadowHasInvalidAttribute(key: String, value: String) extends Failure {
    override def description = s"Product shadow has an invalid attribute $key with value $value"
  }

  final case class ProductShadowAttributeNotAString(key: String) extends Failure {
    override def description = s"Product shadow attribute $key must be a string"
  }

  final case object ProductAttributesAreEmpty extends Failure {
    override def description = s"Product attributes are empty"
  }

  final case object ProductShadowAttributesAreEmpty extends Failure {
    override def description = s"Product shadow attributes are empty"
  }

  object ProductFormNotFound { 
    def apply(id: Int) = NotFoundFailure404(s"Product Form with id $id cannot be found")
  }

  final case class NoVariantForContext(context: String) extends Failure {
    override def description = s"No variant context $context"
  }

}
