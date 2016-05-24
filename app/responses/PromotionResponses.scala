package responses

import java.time.Instant

import models.Aliases.Json
import models.discount._
import models.objects._
import models.promotion._
import responses.DiscountResponses._
import responses.ObjectResponses.ObjectContextResponse

object PromotionResponses {

  object PromotionFormResponse {

    case class Root(id: Int, attributes: Json, createdAt: Instant) extends ResponseItem

    def build(f: ObjectForm): Root =
      Root(id = f.id, attributes = f.attributes, createdAt = f.createdAt)
  }

  object PromotionAndDiscountFormResponse {

    case class Root(
        id: Int, attributes: Json, discounts: Seq[DiscountFormResponse.Root], createdAt: Instant)
        extends ResponseItem

    def build(f: ObjectForm, discounts: Seq[ObjectForm]): Root =
      Root(id = f.id,
           attributes = f.attributes,
           discounts = discounts.map(d ⇒ DiscountFormResponse.build(d)),
           createdAt = f.createdAt)
  }

  object PromotionShadowResponse {

    case class Root(id: Int,
                    formId: Int,
                    attributes: Json,
                    discounts: Seq[DiscountShadowResponse.Root],
                    createdAt: Instant)
        extends ResponseItem

    def build(s: ObjectShadow, discounts: Seq[ObjectShadow]): Root =
      Root(id = s.id,
           formId = s.formId,
           attributes = s.attributes,
           discounts = discounts.map(d ⇒ DiscountShadowResponse.build(d)),
           createdAt = s.createdAt)
  }

  object PromotionResponse {
    case class Root(applyType: Promotion.ApplyType,
                    form: PromotionAndDiscountFormResponse.Root,
                    shadow: PromotionShadowResponse.Root)
        extends ResponseItem

    def build(promotion: Promotion,
              f: ObjectForm,
              s: ObjectShadow,
              discountForms: Seq[ObjectForm],
              discountShadows: Seq[ObjectShadow]): Root =
      Root(applyType = promotion.applyType,
           form = PromotionAndDiscountFormResponse.build(f, discountForms),
           shadow = PromotionShadowResponse.build(s, discountShadows))
  }

  object IlluminatedPromotionResponse {

    case class Root(id: Int,
                    context: ObjectContextResponse.Root,
                    applyType: Promotion.ApplyType,
                    attributes: Json,
                    discounts: Seq[IlluminatedDiscountResponse.Root])
        extends ResponseItem

    def build(promotion: IlluminatedPromotion, discounts: Seq[IlluminatedDiscount]): Root =
      Root(id = promotion.id,
           context = ObjectContextResponse.build(promotion.context),
           applyType = promotion.applyType,
           attributes = promotion.attributes,
           discounts = discounts.map(d ⇒ IlluminatedDiscountResponse.build(d)))
  }
}
