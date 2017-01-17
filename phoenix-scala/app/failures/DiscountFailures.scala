package failures

object DiscountFailures {

  case class DiscountNotFoundAtCommit(id: Int, commit: Int) extends Failure {
    override def description = s"Discount $id not with at commit $commit"
  }

  object DiscountShadowNotFoundInPayload {
    def apply(code: String) =
      NotFoundFailure404(s"Discount shadow with code $code not found in payload")
  }

  object DiscountNotFoundForContext {
    def apply(discountId: Int, discountContextId: Int) =
      NotFoundFailure404(
        s"Discount with id=$discountId with discount context $discountContextId cannot be found")
  }

  case class DiscountShadowHasInvalidAttribute(key: String, value: String) extends Failure {
    override def description = s"Discount shadow has an invalid attribute $key with value $value"
  }

  case class DiscountShadowAttributeNotAString(key: String) extends Failure {
    override def description = s"Discount shadow attribute $key must be a string"
  }

  case object DiscountAttributesAreEmpty extends Failure {
    override def description = "Discount attributes are empty"
  }

  case object DiscountShadowAttributesAreEmpty extends Failure {
    override def description = "Discount shadow attributes are empty"
  }

  object DiscountFormNotFound {
    def apply(id: Int) = NotFoundFailure404(s"Discount Form with id $id cannot be found")
  }

  // TBD - more friendly
  case object SearchFailure extends Failure {
    override def description = "No matching items found during search"
  }
}
