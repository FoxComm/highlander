package payloads

import models.Aliases.Json
import models.promotion.Promotion.ApplyType
import payloads.DiscountPayloads.{CreateDiscountForm, CreateDiscountShadow}

object PromotionPayloads {

  case class UpdatePromoDiscountForm(id: Int, attributes: Json)

  case class UpdatePromoDiscountShadow(id: Int, attributes: Json)

  case class CreatePromotionForm(attributes: Json, discounts: Seq[CreateDiscountForm])

  case class CreatePromotionShadow(attributes: Json, discounts: Seq[CreateDiscountShadow])

  case class CreatePromotion(
      applyType: ApplyType, form: CreatePromotionForm, shadow: CreatePromotionShadow)

  case class UpdatePromotionForm(attributes: Json, discounts: Seq[UpdatePromoDiscountForm])

  case class UpdatePromotionShadow(attributes: Json, discounts: Seq[UpdatePromoDiscountShadow])

  case class UpdatePromotion(
      applyType: ApplyType, form: UpdatePromotionForm, shadow: UpdatePromotionShadow)
}
