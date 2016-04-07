package failures

import models.coupon.CouponCodes

object CouponFailures {

  object CouponNotFound { 
    def apply(id: Int) = NotFoundFailure404(s"Coupon $id not found")
  }

  final case class CouponNotFoundAtCommit(id: Int, commit: Int) extends Failure {
    override def description = s"Coupon $id not with at commit $commit"
  }

  final case class CouponCodePrefixNotSet() extends Failure {
    override def description = s"Coupon code prefix is not set"
  }

  final case class CouponCodeQuanityMustBeGreaterThanZero() extends Failure {
    override def description = s"Coupon code quantity must be greater than zero"
  }

  final case class CouponCodeLengthIsTooSmall(prefix: String, quantity: Int) extends Failure {
    val minSize = CouponCodes.charactersGivenQuantity(quantity) + prefix.length
    override def description = s"Coupon code character length must be at least $minSize"
  }

  object CouponShadowNotFoundInPayload { 
    def apply(code: String)  = NotFoundFailure404(s"Coupon shadow with code $code not found in payload")
  }

  object CouponNotFoundForContext { 
    def apply(couponId: Int, contextName: String) =
      NotFoundFailure404(s"Coupon with id=$couponId with coupon context $contextName cannot be found")
  }

  final case class CouponShadowHasInvalidAttribute(key: String, value: String) extends Failure {
    override def description = s"Coupon shadow has an invalid attribute $key with value $value"
  }

  final case class CouponShadowAttributeNotAString(key: String) extends Failure {
    override def description = s"Coupon shadow attribute $key must be a string"
  }

  final case object CouponAttributesAreEmpty extends Failure {
    override def description = s"Coupon attributes are empty"
  }

  final case object CouponShadowAttributesAreEmpty extends Failure {
    override def description = s"Coupon shadow attributes are empty"
  }

  object CouponFormNotFound { 
    def apply(id: Int) = NotFoundFailure404(s"Coupon Form with id $id cannot be found")
  }


}
