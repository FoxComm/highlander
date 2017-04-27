package responses

import io.circe.syntax._
import java.time.Instant
import models.coupon._
import models.objects._
import responses.ObjectResponses.ObjectContextResponse
import utils.aliases._
import utils.json.codecs._

object CouponResponses {

  object CouponFormResponse {
    case class Root(id: Int, attributes: Json, createdAt: Instant) extends ResponseItem {
      def json: Json = this.asJson
    }

    def build(f: ObjectForm): Root =
      Root(id = f.id, attributes = f.attributes, createdAt = f.createdAt)
  }

  object CouponCodesResponse {

    case class Root(code: String, createdAt: Instant) extends ResponseItem {
      def json: Json = this.asJson
    }

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
        extends ResponseItem {
      def json: Json = this.asJson
    }

    def build(coupon: IlluminatedCoupon, originalCoupon: Coupon): Root =
      Root(id = coupon.id,
           context = ObjectContextResponse.build(coupon.context),
           attributes = coupon.attributes,
           promotion = coupon.promotion,
           archivedAt = originalCoupon.archivedAt)

    def build(context: ObjectContext,
              coupon: Coupon,
              form: ObjectForm,
              shadow: ObjectShadow): Root =
      build(IlluminatedCoupon.illuminate(context, coupon, form, shadow), coupon)

  }
}
