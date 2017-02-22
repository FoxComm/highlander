package failures

object PromotionFailures {

  case class PromotionNotFoundAtCommit(id: Int, commit: Int) extends Failure {
    override def description = s"Promotion $id not with at commit $commit"
  }

  object PromotionShadowNotFoundInPayload {
    def apply(code: String) =
      NotFoundFailure404(s"Promotion shadow with code $code not found in payload")
  }

  case object PromotionIsNotActive extends Failure {
    override def description = "Promotion is not active"
  }

  case class PromotionShadowHasInvalidAttribute(key: String, value: String) extends Failure {
    override def description = s"Promotion shadow has an invalid attribute $key with value $value"
  }

  case class PromotionShadowAttributeNotAString(key: String) extends Failure {
    override def description = s"Promotion shadow attribute $key must be a string"
  }

  case object PromotionAttributesAreEmpty extends Failure {
    override def description = "Promotion attributes are empty"
  }

  case object PromotionShadowAttributesAreEmpty extends Failure {
    override def description = "Promotion shadow attributes are empty"
  }

  case object OrderHasNoPromotions extends Failure {
    override def description = "Order has no promotions"
  }

  object PromotionFormNotFound {
    def apply(id: Int) = NotFoundFailure404(s"Promotion Form with id $id cannot be found")
  }
}
