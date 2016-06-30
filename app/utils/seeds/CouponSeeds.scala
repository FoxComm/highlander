package utils.seeds

import scala.concurrent.ExecutionContext.Implicits.global

import models.coupon._
import models.objects._
import models.product.SimpleContext
import payloads.CouponPayloads._
import slick.driver.PostgresDriver.api._
import utils.aliases.AC
import utils.db._

trait CouponSeeds {

  val codePrefix = "BASE"
  val codeLength = 15
  val codesQty   = 10

  def createCoupons(promotions: Seq[BasePromotion])(implicit db: Database,
                                                    ac: AC): DbResultT[Seq[BaseCoupon]] =
    for {
      context ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
      results ← * <~ promotions.map { promotion ⇒
                 val payload = createCoupon(promotion)
                 insertCoupon(promotion, payload, context)
               }
    } yield results

  def insertCoupon(promo: BasePromotion, payload: CreateCoupon, context: ObjectContext)(
      implicit db: Database,
      ac: AC): DbResultT[BaseCoupon] =
    for {
      // Create coupon
      form   ← * <~ ObjectForm(kind = Coupon.kind, attributes = payload.form.attributes)
      shadow ← * <~ ObjectShadow(attributes = payload.shadow.attributes)
      ins    ← * <~ ObjectUtils.insert(form, shadow)
      coupon ← * <~ Coupons.create(
                  Coupon(contextId = context.id,
                         formId = ins.form.id,
                         shadowId = ins.shadow.id,
                         commitId = ins.commit.id,
                         promotionId = payload.promotion))
      // Generate codes for it
      codes ← * <~ CouponCodes.generateCodes(codePrefix, codeLength, codesQty)
      unsaved = codes.map { c ⇒
        CouponCode(couponFormId = ins.form.id, code = c)
      }
      couponCodes ← * <~ CouponCodes.createAll(unsaved)
    } yield BaseCoupon(form.id, shadow.id, promo.promotionId)

  def createCoupon(promotion: BasePromotion): CreateCoupon = {
    val promotionForm   = BaseCouponForm(promotion.title)
    val promotionShadow = BaseCouponShadow(promotionForm)

    CreateCoupon(form = CreateCouponForm(attributes = promotionForm.form),
                 shadow = CreateCouponShadow(attributes = promotionShadow.shadow),
                 promotion = promotion.formId)
  }
}
