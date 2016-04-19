package utils.seeds.generators

import models.product.SimpleContext
import models.coupon._
import models.objects._
import services.Result
import utils.DbResultT
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._

import payloads.{CreateCoupon, CreateCouponForm, CreateCouponShadow,
  CreateDiscountForm, CreateDiscountShadow}
import services.coupon.CouponManager

import cats.data.Xor
import java.time.Instant
import org.json4s.JsonAST.{JValue, JString, JObject, JField, JNothing}
import org.json4s._
import org.json4s.jackson.JsonMethods._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import scala.util.Random
import slick.driver.PostgresDriver.api._

object SimpleCoupon {
  type Percent = Int
}
import SimpleCoupon._

case class SimpleCoupon(formId: Int = 0, shadowId: Int = 0,  
  percentOff: Percent, totalAmount: Int, promotionId: Int)

case class SimpleCouponForm(percentOff: Percent, totalAmount: Int) {

    val (keyMap, form) = ObjectUtils.createForm(parse(s"""
    {
      "name" : "${percentOff}% off over $totalAmount",
      "storefrontName" : "Get ${percentOff}% off over $totalAmount dollars",
      "description" : "Get ${percentOff}% full order after spending more than $totalAmount dollars",
      "details" : "This offer applies only when you have a total amount $totalAmount dollars",
      "activeFrom" : "${Instant.now}",
      "activeTo" : null,
      "tags" : []
      }
    }"""))
}

case class SimpleCouponShadow(f: SimpleCouponForm) { 

    val shadow = ObjectUtils.newShadow(parse(
      """
        {
          "name" : {"type": "string", "ref": "name"},
          "storefrontName" : {"type": "richText", "ref": "storefrontName"},
          "description" : {"type": "richText", "ref": "description"},
          "details" : {"type": "richText", "ref": "details"},
          "activeFrom" : {"type": "date", "ref": "activeFrom"},
          "activeTo" : {"type": "date", "ref": "activeTo"},
          "tags" : {"type": "tags", "ref": "tags"}
        }"""), 
      f.keyMap)
}

trait CouponGenerator {

  def generateCoupon(promotion: SimplePromotion): SimpleCoupon = {
    SimpleCoupon(
      percentOff = promotion.percentOff,
      totalAmount = promotion.totalAmount,
      promotionId = promotion.promotionId)
  }

  def generateCoupons(data: Seq[SimpleCoupon])(implicit db: Database) = for {
    context ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
    coupons ← * <~ DbResultT.sequence(data.map( d ⇒  {
        val couponForm = SimpleCouponForm(d.percentOff, d.totalAmount)
        val couponShadow = SimpleCouponShadow(couponForm)
        val payload = CreateCoupon(
          form = CreateCouponForm(attributes = couponForm.form),
          shadow = CreateCouponShadow(attributes = couponShadow.shadow),
          d.promotionId)
        DbResultT(DBIO.from(CouponManager.create(payload, context.name).flatMap { 
          case Xor.Right(r) ⇒ Result.right(d.copy(formId = r.form.id, shadowId = r.shadow.id))
          case Xor.Left(l) ⇒  Result.failures(l)
        }))
    }))
  } yield coupons

}
