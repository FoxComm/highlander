package utils.seeds

import scala.concurrent.ExecutionContext.Implicits.global

import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.numeric.Positive
import models.coupon._
import models.account._
import models.objects._
import models.product.SimpleContext
import utils.aliases._
import utils.db._

object CouponSeeds {
  // TODO: migrate to new payloads. // narma  22.09.16
  case class CreateCouponForm(attributes: Json)
  case class CreateCouponShadow(attributes: Json)
  case class CreateCoupon(form: CreateCouponForm,
                          shadow: CreateCouponShadow,
                          promotion: Int,
                          scope: Option[String] = None)
}

trait CouponSeeds {

  val codePrefix = refineMV[NonEmpty]("BASE")
  val codeLength = refineMV[Positive](15)
  val codesQty   = refineMV[Positive](10)

  import CouponSeeds._

  def createCoupons(promotions: Seq[BasePromotion])(
      implicit db: DB,
      ac: AC,
      au: AU): DbResultT[Seq[(BaseCoupon, Seq[CouponCode])]] =
    for {
      context ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
      results ← * <~ promotions.map { promotion ⇒
                 val payload = createCoupon(promotion)
                 insertCoupon(promotion, payload, context)
               }
    } yield results

  private def insertCoupon(promo: BasePromotion, payload: CreateCoupon, context: ObjectContext)(
      implicit db: DB,
      ac: AC,
      au: AU): DbResultT[(BaseCoupon, Seq[CouponCode])] =
    for {
      // Create coupon
      scope  ← * <~ Scope.resolveOverride(payload.scope)
      form   ← * <~ ObjectForm(kind = Coupon.kind, attributes = payload.form.attributes)
      shadow ← * <~ ObjectShadow(attributes = payload.shadow.attributes)
      ins    ← * <~ ObjectUtils.insert(form, shadow, None)
      coupon ← * <~ Coupons.create(
                  Coupon(scope = scope,
                         contextId = context.id,
                         formId = ins.form.id,
                         shadowId = ins.shadow.id,
                         commitId = ins.commit.id,
                         promotionId = payload.promotion))
      // Generate codes for it
      codes ← * <~ CouponCodes.generateCodes(codePrefix, codeLength, codesQty)
      unsaved = codes.map { c ⇒
        CouponCode(couponFormId = ins.form.id, code = c)
      }
      couponCodes ← * <~ CouponCodes.createAllReturningModels(unsaved)
    } yield (BaseCoupon(ins.form.id, ins.shadow.id, promo.promotionId), couponCodes)

  private def createCoupon(promotion: BasePromotion): CreateCoupon = {
    val promotionForm   = BaseCouponForm(promotion.title)
    val promotionShadow = BaseCouponShadow(promotionForm)

    CreateCoupon(form = CreateCouponForm(attributes = promotionForm.form),
                 shadow = CreateCouponShadow(attributes = promotionShadow.shadow),
                 promotion = promotion.formId)
  }
}
