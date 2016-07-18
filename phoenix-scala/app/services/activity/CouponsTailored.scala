package services.activity

import models.coupon.Coupon
import responses.CouponResponses.CouponResponse
import responses.StoreAdminResponse

object CouponsTailored {
  case class CouponCreated(coupon: CouponResponse.Root, admin: Option[StoreAdminResponse.Root])
      extends ActivityBase[CouponCreated]

  case class CouponUpdated(coupon: CouponResponse.Root, admin: Option[StoreAdminResponse.Root])
      extends ActivityBase[CouponUpdated]

  case class SingleCouponCodeGenerated(coupon: Coupon, admin: Option[StoreAdminResponse.Root])
      extends ActivityBase[SingleCouponCodeGenerated]

  case class MultipleCouponCodesGenerated(coupon: Coupon, admin: Option[StoreAdminResponse.Root])
      extends ActivityBase[MultipleCouponCodesGenerated]
}
