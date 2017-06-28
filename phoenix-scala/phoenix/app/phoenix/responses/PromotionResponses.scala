package phoenix.responses

import java.time.Instant

import objectframework.ObjectResponses.ObjectContextResponse
import objectframework.models._
import phoenix.models.discount._
import phoenix.models.promotion._
import phoenix.responses.DiscountResponses._
import phoenix.utils.aliases._

object PromotionResponses {

  case class PromotionFormResponse(id: Int, attributes: Json, createdAt: Instant) extends ResponseItem

  object PromotionFormResponse {

    def build(f: ObjectForm): PromotionFormResponse =
      PromotionFormResponse(id = f.id, attributes = f.attributes, createdAt = f.createdAt)
  }

  case class PromotionResponse(id: Int,
                               context: ObjectContextResponse,
                               applyType: Promotion.ApplyType,
                               attributes: Json,
                               discounts: Seq[IlluminatedDiscountResponse],
                               archivedAt: Option[Instant])
      extends ResponseItem

  object PromotionResponse {

    def build(promotion: IlluminatedPromotion,
              discounts: Seq[IlluminatedDiscount],
              promo: Promotion): PromotionResponse =
      PromotionResponse(
        id = promotion.id,
        context = ObjectContextResponse.build(promotion.context),
        applyType = promotion.applyType,
        attributes = promotion.attributes,
        discounts = discounts.map(d ⇒ IlluminatedDiscountResponse.build(d)),
        archivedAt = promo.archivedAt
      )

    def build(context: ObjectContext,
              promotion: Promotion,
              form: ObjectForm,
              shadow: ObjectShadow,
              discounts: Seq[(ObjectForm, ObjectShadow)]): PromotionResponse = {
      val promoIlluminated = IlluminatedPromotion.illuminate(context, promotion, form, shadow)
      build(promotion = promoIlluminated, discounts = discounts.map {
        case (f, s) ⇒
          IlluminatedDiscount.illuminate(form = f, shadow = s)
      }, promo = promotion)
    }

  }
}
