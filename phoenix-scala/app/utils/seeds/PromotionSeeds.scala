package utils.seeds

import scala.concurrent.ExecutionContext.Implicits.global

import models.account._
import models.objects._
import models.product.SimpleContext
import models.promotion.Promotion.ApplyType
import models.promotion.{Promotion, Promotions}
import utils.aliases._
import utils.db._

object PromotionSeeds {
  // TODO: migrate to new payloads. // narma  22.09.16
  import utils.seeds.DiscountSeeds._

  case class UpdatePromoDiscountForm(id: Int, attributes: Json)
  case class UpdatePromoDiscountShadow(id: Int, attributes: Json)
  case class CreatePromotionForm(attributes: Json, discounts: Seq[CreateDiscountForm])
  case class CreatePromotionShadow(attributes: Json, discounts: Seq[CreateDiscountShadow])
  case class UpdatePromoDiscount(id: Int, attributes: Map[String, Json])

  case class CreatePromotion(applyType: ApplyType,
                             form: CreatePromotionForm,
                             shadow: CreatePromotionShadow,
                             scope: Option[String] = None)

  case class UpdatePromotionForm(attributes: Json, discounts: Seq[UpdatePromoDiscountForm])
  case class UpdatePromotionShadow(attributes: Json, discounts: Seq[UpdatePromoDiscountShadow])
}

trait PromotionSeeds {
  import PromotionSeeds._

  def createCouponPromotions(discounts: Seq[BaseDiscount])(implicit db: DB,
                                                           ac: AC,
                                                           au: AU): DbResultT[Seq[BasePromotion]] =
    for {
      context ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
      results ← * <~ discounts.map { discount ⇒
        val payload = createPromotion(discount.title, Promotion.Coupon)
        insertPromotion(payload, discount, context)
      }
    } yield results

  def insertPromotion(payload: CreatePromotion, discount: BaseDiscount, context: ObjectContext)(
      implicit db: DB,
      ac: AC,
      au: AU): DbResultT[BasePromotion] =
    for {
      scope  ← * <~ Scope.resolveOverride(payload.scope)
      form   ← * <~ ObjectForm(kind = Promotion.kind, attributes = payload.form.attributes)
      shadow ← * <~ ObjectShadow(attributes = payload.shadow.attributes)
      ins    ← * <~ ObjectUtils.insert(form, shadow, None)
      promotion ← * <~ Promotions.create(
        Promotion(scope = scope,
                  contextId = context.id,
                  applyType = payload.applyType,
                  formId = ins.form.id,
                  shadowId = ins.shadow.id,
                  commitId = ins.commit.id))
      link ← * <~ PromotionDiscountLinks.create(
        PromotionDiscountLink(leftId = promotion.id, rightId = discount.discountId))
    } yield
      BasePromotion(promotion.id, ins.form.id, ins.shadow.id, payload.applyType, discount.title)

  def createPromotion(name: String, applyType: Promotion.ApplyType): CreatePromotion = {
    val promotionForm   = BasePromotionForm(name, applyType)
    val promotionShadow = BasePromotionShadow(promotionForm)

    CreatePromotion(
      applyType = applyType,
      form = CreatePromotionForm(attributes = promotionForm.form, discounts = Seq.empty),
      shadow = CreatePromotionShadow(attributes = promotionShadow.shadow, discounts = Seq.empty))
  }
}
