package responses

import models.objects._
import models.coupon._
import models.Aliases.Json

import responses.ObjectResponses.ObjectContextResponse

import org.json4s.DefaultFormats
import org.json4s.Extraction
import org.json4s.JsonAST.JValue
import org.json4s.JsonDSL._
import org.json4s.jackson.Serialization.{write ⇒ render}

import java.time.Instant
import cats.implicits._

object CouponResponses {

  object CouponFormResponse {

    final case class Root(id: Int, attributes: Json, createdAt: Instant) extends ResponseItem

    def build(f: ObjectForm): Root = 
      Root(id = f.id, attributes = f.attributes, createdAt = f.createdAt)
  }

  object CouponShadowResponse {

    final case class Root(id: Int, formId: Int, attributes: Json, createdAt: Instant) extends ResponseItem

    def build(s: ObjectShadow): Root = 
      Root(id = s.id, formId = s.formId, attributes = s.attributes, 
        createdAt = s.createdAt)
  }

  object CouponResponse { 

    final case class Root(id: Int, form: CouponFormResponse.Root, 
      shadow: CouponShadowResponse.Root, promotion: Int) extends ResponseItem

    def build(coupon: Coupon, f: ObjectForm, s: ObjectShadow) : Root = Root(
        id = coupon.formId, form = CouponFormResponse.build(f), 
        shadow = CouponShadowResponse.build(s), promotion = coupon.promotionId)
  }

  object CouponCodesResponse { 

    final case class Root(code: String, createdAt: Instant) extends ResponseItem

    def build(codes: Seq[CouponCode]) : Seq[Root] = 
      codes.map{ c ⇒ Root(code = c.code, createdAt = c.createdAt)}
  }

  object IlluminatedCouponResponse {

    final case class Root(id: Int, context: ObjectContextResponse.Root, 
      attributes: Json, promotion: Int) extends ResponseItem

    def build(coupon: IlluminatedCoupon): Root = 
      Root(id = coupon.id, context = ObjectContextResponse.build(coupon.context), 
        attributes = coupon.attributes, promotion = coupon.promotion)
  }
}
