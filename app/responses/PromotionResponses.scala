package responses

import models.objects._
import models.promotion._
import models.discount._
import models.Aliases.Json

import responses.ObjectResponses.ObjectContextResponse
import responses.DiscountResponses._

import org.json4s.DefaultFormats
import org.json4s.Extraction
import org.json4s.JsonAST.JValue
import org.json4s.JsonDSL._
import org.json4s.jackson.Serialization.{write ⇒ render}

import java.time.Instant
import cats.implicits._

object PromotionResponses {

  object PromotionFormResponse {

    final case class Root(id: Int, attributes: Json, createdAt: Instant)

    def build(f: ObjectForm): Root = 
      Root(id = f.id, attributes = f.attributes, createdAt = f.createdAt)
  }

  object PromotionAndDiscountFormResponse {

    final case class Root(id: Int, attributes: Json, 
      discounts: Seq[DiscountFormResponse.Root], createdAt: Instant)

    def build(f: ObjectForm, discounts: Seq[ObjectForm]): Root = 
      Root(id = f.id, attributes = f.attributes, 
        discounts = discounts.map(d ⇒ DiscountFormResponse.build(d)), 
        createdAt = f.createdAt)
  }

  object PromotionShadowResponse {

    final case class Root(id: Int, formId: Int, attributes: Json, 
      discounts: Seq[DiscountShadowResponse.Root], createdAt: Instant)

    def build(s: ObjectShadow, discounts: Seq[ObjectShadow]): Root = 
      Root(id = s.id, formId = s.formId, attributes = s.attributes, 
        discounts = discounts.map(d ⇒ DiscountShadowResponse.build(d)),
        createdAt = s.createdAt)
  }

  object PromotionResponse { 
    final case class Root(applyType: Promotion.ApplyType, form: PromotionAndDiscountFormResponse.Root, 
      shadow: PromotionShadowResponse.Root)

    def build(promotion: Promotion, f: ObjectForm, s: ObjectShadow, discountForms: Seq[ObjectForm], 
      discountShadows: Seq[ObjectShadow]) : Root = Root(
        applyType = promotion.applyType,
        form = PromotionAndDiscountFormResponse.build(f, discountForms), 
        shadow = PromotionShadowResponse.build(s, discountShadows))
  }

  object IlluminatedPromotionResponse {

    final case class Root(id: Int, context: ObjectContextResponse.Root, applyType: Promotion.ApplyType, 
      attributes: Json, discounts: Seq[IlluminatedDiscountResponse.Root])

    def build(promotion: IlluminatedPromotion, discounts: Seq[IlluminatedDiscount]): Root = 
      Root(id = promotion.id, context = ObjectContextResponse.build(promotion.context), 
        applyType = promotion.applyType, attributes = promotion.attributes, 
        discounts = discounts.map(d ⇒ IlluminatedDiscountResponse.build(d)))
  }
}
