package utils.seeds

import scala.concurrent.ExecutionContext.Implicits.global
import com.github.tminglei.slickpg.LTree

import models.coupon._
import models.objects._
import models.product.SimpleContext
import payloads.CouponPayloads._
import utils.aliases._
import utils.db._

trait CouponSeeds {

  val codePrefix = "BASE"
  val codeLength = 15
  val codesQty   = 10

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

  def insertCoupon(promo: BasePromotion, payload: CreateCoupon, context: ObjectContext)(
      implicit db: DB,
      ac: AC,
      au: AU): DbResultT[(BaseCoupon, Seq[CouponCode])] =
    for {
      // Create coupon
      form   ← * <~ ObjectForm(kind = Coupon.kind, attributes = payload.form.attributes)
      shadow ← * <~ ObjectShadow(attributes = payload.shadow.attributes)
      ins    ← * <~ ObjectUtils.insert(form, shadow)
      coupon ← * <~ Coupons.create(
                  Coupon(scope = LTree(au.token.scope),
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
    } yield (BaseCoupon(form.id, shadow.id, promo.promotionId), couponCodes)

  def createCoupon(promotion: BasePromotion): CreateCoupon = {
    val promotionForm   = BaseCouponForm(promotion.title)
    val promotionShadow = BaseCouponShadow(promotionForm)

    CreateCoupon(form = CreateCouponForm(attributes = promotionForm.form),
                 shadow = CreateCouponShadow(attributes = promotionShadow.shadow),
                 promotion = promotion.formId)
  }
}
