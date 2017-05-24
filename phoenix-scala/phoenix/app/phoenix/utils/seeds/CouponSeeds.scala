package phoenix.utils.seeds

import objectframework.ObjectUtils
import objectframework.models._
import org.json4s.Formats
import phoenix.models.account._
import phoenix.models.coupon._
import phoenix.models.product.SimpleContext
import phoenix.utils.JsonFormatters
import phoenix.utils.aliases._
import utils.db._

import scala.concurrent.ExecutionContext.Implicits.global

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

  implicit val formats: Formats = JsonFormatters.phoenixFormats

  val codePrefix = "BASE"
  val codeLength = 15
  val codesQty   = 10

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
