package phoenix.responses

import java.time.Instant

import objectframework.ObjectResponses.ObjectContextResponse
import objectframework.models._
import phoenix.models.coupon._
import phoenix.utils.aliases._

object CouponResponses {

  object CouponFormResponse {
    case class Root(id: Int, attributes: Json, createdAt: Instant) extends ResponseItem

    def build(f: ObjectForm): Root =
      Root(id = f.id, attributes = f.attributes, createdAt = f.createdAt)
  }

  object CouponCodesResponse {

    case class Root(code: String, createdAt: Instant) extends ResponseItem

    def build(codes: Seq[CouponCode]): Seq[Root] =
      codes.map { c ⇒
        Root(code = c.code, createdAt = c.createdAt)
      }
  }

  object CouponResponse {

    case class Root(id: Int,
                    context: ObjectContextResponse.Root,
                    attributes: Json,
                    promotion: Int,
                    archivedAt: Option[Instant])
        extends ResponseItem

    def build(coupon: IlluminatedCoupon, originalCoupon: Coupon): Root =
      Root(
        id = coupon.id,
        context = ObjectContextResponse.build(coupon.context),
        attributes = coupon.attributes,
        promotion = coupon.promotion,
        archivedAt = originalCoupon.archivedAt
      )

    def build(context: ObjectContext, coupon: Coupon, form: ObjectForm, shadow: ObjectShadow): Root =
      build(IlluminatedCoupon.illuminate(context, coupon, form, shadow), coupon)

  }
}
