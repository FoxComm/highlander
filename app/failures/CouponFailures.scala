package failures

import models.coupon.CouponCodes

object CouponFailures {

  object CouponNotFound {
    def apply(id: Int) = NotFoundFailure404(s"Coupon $id not found")
  }

  object CouponWithCodeCannotBeFound {
    def apply(code: String) = NotFoundFailure404(s"Coupon with code $code not found")
  }

  case class CouponNotFoundAtCommit(id: Int, commit: Int) extends Failure {
    override def description = s"Coupon $id not with at commit $commit"
  }

  case object CouponCodePrefixNotSet extends Failure {
    override def description = "Coupon code prefix is not set"
  }

  case object CouponCodeQuanityMustBeGreaterThanZero extends Failure {
    override def description = "Coupon code quantity must be greater than zero"
  }

  case class CouponCodeLengthIsTooSmall(prefix: String, quantity: Int) extends Failure {
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

  case class CouponShadowHasInvalidAttribute(key: String, value: String) extends Failure {
    override def description = s"Coupon shadow has an invalid attribute $key with value $value"
  }

  case class CouponShadowAttributeNotAString(key: String) extends Failure {
    override def description = s"Coupon shadow attribute $key must be a string"
  }

  case object CouponAttributesAreEmpty extends Failure {
    override def description = "Coupon attributes are empty"
  }

  case object CouponShadowAttributesAreEmpty extends Failure {
    override def description = "Coupon shadow attributes are empty"
  }

  object CouponFormNotFound {
    def apply(id: Int) = NotFoundFailure404(s"Coupon Form with id $id cannot be found")
  }


}
