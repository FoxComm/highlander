package failures

import models.product.Product

object ProductFailures {

  object ProductVariantNotFound {
    def apply(code: String) = NotFoundFailure404(s"Product variant $code not found")
    def apply(id: Int)      = NotFoundFailure404(s"Product variant with id $id not found")
  }

  object ProductVariantWithFormNotFound {
    def apply(formId: Int) = NotFoundFailure404(s"Product variant with form id $formId not found")
  }

  object ProductVariantWithShadowNotFound {
    def apply(shadowId: Int) =
      NotFoundFailure404(s"Product variant with shadow id $shadowId not found")
  }

  case class ProductNotFoundAtCommit(id: Int, commit: Int) extends Failure {
    override def description = s"Product $id not with at commit $commit"
  }

  object ProductVariantShadowNotFoundInPayload {
    def apply(code: String) =
      NotFoundFailure404(s"Product variant shadow with code $code not found in payload")
  }

  object ProductVariantNotFoundForContext {
    def apply(code: String, productContextId: Int) =
      NotFoundFailure404(
          s"ProductVariant $code with product context $productContextId cannot be found")
  }

  object ProductOptionNotFound {
    def apply(id: Int) = NotFoundFailure404(s"Product option with id $id not found")
  }

  object ProductOptionNotFoundForContext {
    def apply(id: Int, contextId: Int) =
      NotFoundFailure404(s"Product option $id with context $contextId cannot be found")
  }

  object ProductValueNotFoundForContext {
    def apply(id: Int, contextId: Int) =
      NotFoundFailure404(s"Product value value $id with context $contextId cannot be found")
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

  case class NoProductFoundForVariant(id: Int) extends Failure {
    override def description = s"No product for variant $id found"
  }

  case class SlugShouldHaveLetters(slugValue: String) extends Failure {
    override def description: String = s"Slug should have at least one letter: '$slugValue'"
  }

  case class SlugDuplicates(slugValue: String) extends Failure {
    override def description: String =
      s"Product slug '$slugValue' is already defined for other product"
  }
}
