package phoenix.services.activity

import phoenix.models.coupon.Coupon
import phoenix.responses.users.UserResponse

object CouponsTailored {
  case class SingleCouponCodeGenerated(coupon: Coupon, admin: Option[UserResponse])
      extends ActivityBase[SingleCouponCodeGenerated]

  case class MultipleCouponCodesGenerated(coupon: Coupon, admin: Option[UserResponse])
      extends ActivityBase[MultipleCouponCodesGenerated]
}
