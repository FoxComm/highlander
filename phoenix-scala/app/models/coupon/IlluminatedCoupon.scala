package models.coupon

import java.time.Instant

import cats.data.Xor
import failures.CouponFailures._
import failures._
import models.objects._
import services.coupon.CouponUsageService
import utils.aliases._
import utils.db._
import utils.{IlluminateAlgorithm, JsonFormatters}

/**
  * An IlluminatedCoupon is what you get when you combine the coupon shadow and
  * the form. 
  */
case class IlluminatedCoupon(id: Int,
                             context: IlluminatedContext,
                             attributes: Json,
                             promotion: Int) {

  implicit val formats = JsonFormatters.phoenixFormats

  def mustBeActive: Failures Xor IlluminatedCoupon = {
    val activeFrom = (attributes \ "activeFrom" \ "v").extractOpt[Instant]
    val activeTo   = (attributes \ "activeTo" \ "v").extractOpt[Instant]
    val now        = Instant.now

    (activeFrom, activeTo) match {
      case (Some(from), Some(to)) ⇒
        if (from.isBefore(now) && to.isAfter(now)) Xor.right(this)
        else Xor.left(CouponIsNotActive.single)
      case (Some(from), None) ⇒
        if (from.isBefore(now)) Xor.right(this) else Xor.left(CouponIsNotActive.single)
      case (_, _) ⇒
        Xor.left(CouponIsNotActive.single)
    }
  }

  def mustBeApplicable(code: CouponCode, customerId: Int)(implicit ec: EC,
                                                          db: DB): DbResultT[IlluminatedCoupon] = {
    val usageRules = (attributes \ "usageRules" \ "v").extractOpt[CouponUsageRules]

    val validation = usageRules match {
      case Some(rules) if !rules.isUnlimitedPerCode && !rules.isUnlimitedPerCustomer ⇒
        CouponUsageService.mustBeUsableByCustomer(id,
                                                  code.id,
                                                  rules.usesPerCode.getOrElse(0),
                                                  customerId,
                                                  rules.usesPerCustomer.getOrElse(0),
                                                  code.code)

      case Some(rules) if !rules.isUnlimitedPerCode && rules.isUnlimitedPerCustomer ⇒
        CouponUsageService
          .couponCodeMustBeUsable(id, code.id, rules.usesPerCode.getOrElse(0), code.code)

      case Some(rules) if rules.isUnlimitedPerCode && !rules.isUnlimitedPerCustomer ⇒
        CouponUsageService
          .couponMustBeUsable(id, customerId, rules.usesPerCustomer.getOrElse(0), code.code)

      case Some(rules) if rules.isUnlimitedPerCode && rules.isUnlimitedPerCustomer ⇒
        DbResultT.unit

      case _ ⇒
        DbResultT.failure(CouponUsageRulesAreEmpty(code.code))
    }

    for (_ ← * <~ validation) yield this
  }
}

object IlluminatedCoupon {

  def illuminate(context: ObjectContext,
                 coupon: Coupon,
                 form: ObjectForm,
                 shadow: ObjectShadow): IlluminatedCoupon = {

    IlluminatedCoupon(id = coupon.formId,
                      promotion = coupon.promotionId,
                      context = IlluminatedContext(context.name, context.attributes),
                      attributes =
                        IlluminateAlgorithm.projectAttributes(form.attributes, shadow.attributes))
  }
}
