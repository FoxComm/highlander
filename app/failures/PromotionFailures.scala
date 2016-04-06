package failures

object PromotionFailures {

  object PromotionNotFound { 
    def apply(id: Int) = NotFoundFailure404(s"Promotion $id not found")
  }

  final case class PromotionNotFoundAtCommit(id: Int, commit: Int) extends Failure {
    override def description = s"Promotion $id not with at commit $commit"
  }

  object PromotionShadowNotFoundInPayload { 
    def apply(code: String)  = NotFoundFailure404(s"Promotion shadow with code $code not found in payload")
  }

  object PromotionNotFoundForContext { 
    def apply(promotionId: Int, contextName: String) =
      NotFoundFailure404(s"Promotion with id=$promotionId with promotion context $contextName cannot be found")
  }

  final case class PromotionShadowHasInvalidAttribute(key: String, value: String) extends Failure {
    override def description = s"Promotion shadow has an invalid attribute $key with value $value"
  }

  final case class PromotionShadowAttributeNotAString(key: String) extends Failure {
    override def description = s"Promotion shadow attribute $key must be a string"
  }

  final case object PromotionAttributesAreEmpty extends Failure {
    override def description = s"Promotion attributes are empty"
  }

  final case object PromotionShadowAttributesAreEmpty extends Failure {
    override def description = s"Promotion shadow attributes are empty"
  }

  object PromotionFormNotFound { 
    def apply(id: Int) = NotFoundFailure404(s"Promotion Form with id $id cannot be found")
  }

}
