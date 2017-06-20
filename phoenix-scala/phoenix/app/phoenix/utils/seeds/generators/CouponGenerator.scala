package phoenix.utils.seeds.generators

import java.time.Instant

import core.db._
import objectframework.ObjectUtils
import objectframework.ObjectUtils._
import objectframework.models._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import phoenix.models.coupon.Coupon
import phoenix.models.product.SimpleContext
import phoenix.payloads.CouponPayloads._
import phoenix.services.coupon.CouponManager
import phoenix.utils.aliases._
import phoenix.utils.seeds.generators.SimpleCoupon._

import scala.concurrent.ExecutionContext.Implicits.global

object SimpleCoupon {
  type Percent = Int
}

case class SimpleCoupon(formId: Int = 0, percentOff: Percent, totalAmount: Int, promotionId: Int)

case class SimpleCouponForm(percentOff: Percent, totalAmount: Int) {

  val (keyMap, form) = ObjectUtils.createForm(parse(s"""
    {
      "name" : "$percentOff% off over $totalAmount",
      "storefrontName" : "Get $percentOff% off over $totalAmount dollars",
      "description" : "Get $percentOff% full order after spending more than $totalAmount dollars",
      "details" : "This offer applies only when you have a total amount $totalAmount dollars",
      "activeFrom" : "${Instant.now}",
      "activeTo" : null,
      "tags" : []
      }
    }"""))
}

case class SimpleCouponShadow(f: SimpleCouponForm) {

  val shadow = ObjectUtils.newShadow(
    parse("""
        {
          "name" : {"type": "string", "ref": "name"},
          "storefrontName" : {"type": "richText", "ref": "storefrontName"},
          "description" : {"type": "text", "ref": "description"},
          "details" : {"type": "richText", "ref": "details"},
          "activeFrom" : {"type": "date", "ref": "activeFrom"},
          "activeTo" : {"type": "date", "ref": "activeTo"},
          "tags" : {"type": "tags", "ref": "tags"}
        }"""),
    f.keyMap
  )
}

trait CouponGenerator {

  def generateCoupon(promotion: SimplePromotion): SimpleCoupon =
    SimpleCoupon(percentOff = promotion.percentOff,
                 totalAmount = promotion.totalAmount,
                 promotionId = promotion.promotionId)

  def generateCoupons(
      sourceData: Seq[SimpleCoupon])(implicit db: DB, ac: AC, au: AU): DbResultT[Seq[SimpleCoupon]] =
    for {
      context ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
      coupons ← * <~ sourceData.map(source ⇒ {
                 val couponForm   = SimpleCouponForm(source.percentOff, source.totalAmount)
                 val couponShadow = SimpleCouponShadow(couponForm)
                 def couponFS: FormAndShadow =
                   (ObjectForm(kind = Coupon.kind, attributes = couponForm.form),
                    ObjectShadow(attributes = couponShadow.shadow))

                 val payload =
                   CreateCoupon(attributes = couponFS.toPayload, promotion = source.promotionId)

                 CouponManager.create(payload, context.name, None).map { newCoupon ⇒
                   source.copy(formId = newCoupon.id)
                 }
               })
    } yield coupons
}
