package phoenix.responses

import java.time.Instant

import objectframework.ObjectResponses.ObjectContextResponse
import objectframework.models._
import phoenix.models.coupon._
import phoenix.utils.aliases._

object CouponResponses {

  case class CouponFormResponse(id: Int, attributes: Json, createdAt: Instant) extends ResponseItem

  object CouponFormResponse {

    def build(f: ObjectForm): CouponFormResponse =
      CouponFormResponse(id = f.id, attributes = f.attributes, createdAt = f.createdAt)
  }

  case class CouponCodesResponse(code: String, createdAt: Instant) extends ResponseItem

  object CouponCodesResponse {

    def build(codes: Seq[CouponCode]): Seq[CouponCodesResponse] =
      codes.map { c ⇒
        CouponCodesResponse(code = c.code, createdAt = c.createdAt)
      }
  }

  case class CouponResponse(id: Int,
                            context: ObjectContextResponse,
                            code: String,
                            attributes: Json,
                            promotion: Int,
                            archivedAt: Option[Instant])
      extends ResponseItem

  object CouponResponse {

    def build(coupon: IlluminatedCoupon, code: String, originalCoupon: Coupon): CouponResponse =
      CouponResponse(
        id = coupon.id,
        context = ObjectContextResponse.build(coupon.context),
        code = code,
        attributes = coupon.attributes,
        promotion = coupon.promotion,
        archivedAt = originalCoupon.archivedAt
      )

    def build(context: ObjectContext,
              code: String,
              coupon: Coupon,
              form: ObjectForm,
              shadow: ObjectShadow): CouponResponse =
      build(IlluminatedCoupon.illuminate(context, coupon, form, shadow), code, coupon)

  }
}
