package payloads

import models.promotion.Promotion.ApplyType
import models.Aliases.Json
import java.time.Instant

final case class UpdatePromoDiscountForm(id: Int, attributes: Json)
final case class UpdatePromoDiscountShadow(id: Int, attributes: Json)
final case class CreatePromotionForm(attributes: Json, discounts: Seq[CreateDiscountForm])
final case class CreatePromotionShadow(attributes: Json, discounts: Seq[CreateDiscountShadow])
final case class CreatePromotion(applyType: ApplyType, form: CreatePromotionForm, shadow: CreatePromotionShadow)
final case class UpdatePromotionForm(attributes: Json, discounts: Seq[UpdatePromoDiscountForm])
final case class UpdatePromotionShadow(attributes: Json, discounts: Seq[UpdatePromoDiscountShadow])
final case class UpdatePromotion(applyType: ApplyType, form: UpdatePromotionForm, shadow: UpdatePromotionShadow)
