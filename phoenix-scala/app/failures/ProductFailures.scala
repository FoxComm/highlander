package failures

import models.product.Product

object ProductFailures {

  object SkuNotFound {
    def apply(code: String) = NotFoundFailure404(s"Sku $code not found")
    def apply(id: Int)      = NotFoundFailure404(s"Sku with id $id not found")
  }

  object SkuWithFormNotFound {
    def apply(formId: Int) = NotFoundFailure404(s"Sku with form id $formId not found")
  }

  object SkuWithShadowNotFound {
    def apply(shadowId: Int) = NotFoundFailure404(s"Sku with shadow id $shadowId not found")
  }

  case class ProductNotFoundAtCommit(id: Int, commit: Int) extends Failure {
    override def description = s"Product $id not with at commit $commit"
  }

  object SkuShadowNotFoundInPayload {
    def apply(code: String) =
      NotFoundFailure404(s"Sku shadow with code $code not found in payload")
  }

  object SkuNotFoundForContext {
    def apply(code: String, productContextId: Int) =
      NotFoundFailure404(s"Sku $code with product context $productContextId cannot be found")
  }

  object VariantNotFound {
    def apply(id: Int) = NotFoundFailure404(s"Variant with id $id not found")
  }

  object VariantNotFoundForContext {
    def apply(id: Int, contextId: Int) =
      NotFoundFailure404(s"Variant $id with context $contextId cannot be found")
  }

  object VariantValueNotFoundForContext {
    def apply(id: Int, contextId: Int) =
      NotFoundFailure404(s"Variant value $id with context $contextId cannot be found")
  }

  object ProductNotFoundForContext {
    def apply(productId: Int, productContextId: Int) =
      NotFoundFailure404(
          s"Product with id=$productId with product context $productContextId cannot be found")

    def apply(slug: String, productContextId: Int) =
      NotFoundFailure404(
          s"Product with slug=$slug with product context $productContextId cannot be found")
  }

  object ProductFormNotFoundForContext {
    def apply(formId: Int, productContextId: Int) =
      NotFoundFailure404(
          s"Product form with id=$formId with product context $productContextId cannot be found")
  }

  object NoAlbumsFoundForProduct {
    def apply(productId: Product#Id) =
      NotFoundFailure404(s"Product with id=$productId has no albums")
  }

  case class ProductShadowHasInvalidAttribute(key: String, value: String) extends Failure {
    override def description = s"Product shadow has an invalid attribute $key with value $value"
  }

  case class ProductShadowAttributeNotAString(key: String) extends Failure {
    override def description = s"Product shadow attribute $key must be a string"
  }

  case object ProductAttributesAreEmpty extends Failure {
    override def description = "Product attributes are empty"
  }

  case object ProductShadowAttributesAreEmpty extends Failure {
    override def description = "Product shadow attributes are empty"
  }

  object ProductFormNotFound {
    def apply(id: Int) = NotFoundFailure404(s"Product Form with id $id cannot be found")
  }

  case class NoVariantForContext(context: String) extends Failure {
    override def description = s"No variant context $context"
  }

  case class NoProductFoundForSku(id: Int) extends Failure {
    override def description = s"No product for SKU $id found"
  }

  case class InvalidSlug(slugValue: String) extends Failure {
    override def description: String = s" '$slugValue' is not valid value for product slug"
  }

  case class SlugDuplicates(slugValue: String) extends Failure {
    override def description: String =
      s"Product slug is already defined for other product: $slugValue"
  }
}
