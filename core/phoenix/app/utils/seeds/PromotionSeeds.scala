package utils.seeds

import scala.concurrent.ExecutionContext.Implicits.global
import com.github.tminglei.slickpg.LTree

import models.objects._
import models.product.SimpleContext
import models.promotion.{Promotion, Promotions}
import payloads.PromotionPayloads._
import utils.aliases._
import utils.db._

trait PromotionSeeds {

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
      form   ← * <~ ObjectForm(kind = Promotion.kind, attributes = payload.form.attributes)
      shadow ← * <~ ObjectShadow(attributes = payload.shadow.attributes)
      ins    ← * <~ ObjectUtils.insert(form, shadow)
      promotion ← * <~ Promotions.create(
                     Promotion(scope = LTree(au.token.scope),
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
