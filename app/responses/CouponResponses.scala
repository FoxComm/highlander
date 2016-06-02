package responses

import java.time.Instant

import models.coupon._
import models.objects._
import responses.ObjectResponses.ObjectContextResponse
import utils.aliases._

object CouponResponses {

  object CouponFormResponse {

    case class Root(id: Int, attributes: Json, createdAt: Instant) extends ResponseItem

    def build(f: ObjectForm): Root =
      Root(id = f.id, attributes = f.attributes, createdAt = f.createdAt)
  }

  object CouponShadowResponse {

    case class Root(id: Int, formId: Int, attributes: Json, createdAt: Instant)
        extends ResponseItem

    def build(s: ObjectShadow): Root =
      Root(id = s.id, formId = s.formId, attributes = s.attributes, createdAt = s.createdAt)
  }

  object CouponResponse {

    case class Root(
        id: Int, form: CouponFormResponse.Root, shadow: CouponShadowResponse.Root, promotion: Int)
        extends ResponseItem

    def build(coupon: Coupon, f: ObjectForm, s: ObjectShadow): Root =
      Root(id = coupon.formId,
           form = CouponFormResponse.build(f),
           shadow = CouponShadowResponse.build(s),
           promotion = coupon.promotionId)
  }

  object CouponCodesResponse {

    case class Root(code: String, createdAt: Instant) extends ResponseItem

    def build(codes: Seq[CouponCode]): Seq[Root] =
      codes.map { c ⇒
        Root(code = c.code, createdAt = c.createdAt)
      }
  }

  object IlluminatedCouponResponse {

    case class Root(id: Int, context: ObjectContextResponse.Root, attributes: Json, promotion: Int)
        extends ResponseItem

    def build(coupon: IlluminatedCoupon): Root =
      Root(id = coupon.id,
           context = ObjectContextResponse.build(coupon.context),
           attributes = coupon.attributes,
           promotion = coupon.promotion)
  }
}
