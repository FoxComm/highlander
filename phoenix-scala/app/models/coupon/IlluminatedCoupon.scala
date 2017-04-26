package models.coupon

import cats.implicits._
import failures.CouponFailures._
import failures._
import java.time.Instant
import models.objects._
import services.coupon.CouponUsageService
import utils.IlluminateAlgorithm
import utils.aliases._
import utils.db._
import utils.json.codecs._

/**
  * An IlluminatedCoupon is what you get when you combine the coupon shadow and
  * the form.
  */
case class IlluminatedCoupon(id: Int,
                             context: IlluminatedContext,
                             attributes: Json,
                             promotion: Int) {

  def mustBeActive: Either[Failures, IlluminatedCoupon] = {
    val attrsC     = attributes.hcursor
    val activeFrom = attrsC.downField("activeFrom").downField("v").as[Instant].toOption
    val activeTo   = attrsC.downField("activeTo").downField("v").as[Instant].toOption
    val now        = Instant.now

    (activeFrom, activeTo) match {
      case (Some(from), Some(to)) ⇒
        if (from.isBefore(now) && to.isAfter(now)) Either.right(this)
        else Either.left(CouponIsNotActive.single)
      case (Some(from), None) ⇒
        if (from.isBefore(now)) Either.right(this) else Either.left(CouponIsNotActive.single)
      case (_, _) ⇒
        Either.left(CouponIsNotActive.single)
    }
  }

  def mustBeApplicable(code: CouponCode, accountId: Int)(implicit ec: EC,
                                                         db: DB): DbResultT[IlluminatedCoupon] = {
    val usageRules =
      attributes.hcursor.downField("usageRules").downField("v").as[CouponUsageRules].toOption

    val validation = usageRules match {
      case Some(rules) if !rules.isUnlimitedPerCode && !rules.isUnlimitedPerCustomer ⇒
        CouponUsageService.mustBeUsableByCustomer(id,
                                                  code.id,
                                                  rules.usesPerCode.getOrElse(0),
                                                  accountId,
                                                  rules.usesPerCustomer.getOrElse(0),
                                                  code.code)

      case Some(rules) if !rules.isUnlimitedPerCode && rules.isUnlimitedPerCustomer ⇒
        CouponUsageService
          .couponCodeMustBeUsable(id, code.id, rules.usesPerCode.getOrElse(0), code.code)

      case Some(rules) if rules.isUnlimitedPerCode && !rules.isUnlimitedPerCustomer ⇒
        CouponUsageService
          .couponMustBeUsable(id, accountId, rules.usesPerCustomer.getOrElse(0), code.code)

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
