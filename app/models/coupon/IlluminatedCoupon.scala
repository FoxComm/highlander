package models.coupon

import java.time.Instant

import cats.data.Xor
import cats.data.Xor._
import failures._
import failures.CouponFailures._
import models.Aliases.Json
import models.objects._
import utils.{IlluminateAlgorithm, JsonFormatters}

/**
 * An IlluminatedCoupon is what you get when you combine the coupon shadow and
 * the form. 
 */
case class IlluminatedCoupon(id: Int, context: IlluminatedContext, 
  attributes: Json, promotion: Int) {

  implicit val formats = JsonFormatters.phoenixFormats

  def mustBeActive: Failures Xor IlluminatedCoupon = {
    val activeFrom = (attributes \ "activeFrom" \ "v").extractOpt[Instant]
    val activeTo   = (attributes \ "activeTo" \ "v").extractOpt[Instant]
    val now = Instant.now

    (activeFrom, activeTo) match {
      case (Some(from), Some(to)) ⇒
        if (from.isBefore(now) && to.isAfter(now)) right(this) else Left(CouponIsNotActive.single)
      case (Some(from), None) ⇒
        if (from.isBefore(now)) right(this) else Left(CouponIsNotActive.single)
      case (_, _) ⇒
        Left(CouponIsNotActive.single)
    }
  }
}

object IlluminatedCoupon { 

  def illuminate(context: ObjectContext, coupon: Coupon, 
    form: ObjectForm, shadow: ObjectShadow) : IlluminatedCoupon = { 

    IlluminatedCoupon(
      id = coupon.formId,  
      promotion = coupon.promotionId,
      context = IlluminatedContext(context.name, context.attributes),
      attributes = IlluminateAlgorithm.projectAttributes(form.attributes, shadow.attributes))
  }
}
