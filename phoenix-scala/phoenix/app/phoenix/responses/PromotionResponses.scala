package phoenix.responses

import java.time.Instant

import objectframework.ObjectResponses.ObjectContextResponse
import objectframework.models._
import phoenix.models.discount._
import phoenix.models.promotion._
import phoenix.responses.DiscountResponses._
import phoenix.utils.aliases._

object PromotionResponses {

  object PromotionFormResponse {

    case class Root(id: Int, attributes: Json, createdAt: Instant) extends ResponseItem

    def build(f: ObjectForm): Root =
      Root(id = f.id, attributes = f.attributes, createdAt = f.createdAt)
  }

  object PromotionResponse {

    case class Root(id: Int,
                    context: ObjectContextResponse.Root,
                    applyType: Promotion.ApplyType,
                    attributes: Json,
                    discounts: Seq[IlluminatedDiscountResponse.Root],
                    archivedAt: Option[Instant])
        extends ResponseItem

    def build(promotion: IlluminatedPromotion, discounts: Seq[IlluminatedDiscount], promo: Promotion): Root =
      Root(
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
              discounts: Seq[(ObjectForm, ObjectShadow)]): Root = {
      val promoIlluminated = IlluminatedPromotion.illuminate(context, promotion, form, shadow)
      build(promotion = promoIlluminated, discounts = discounts.map {
        case (f, s) ⇒
          IlluminatedDiscount.illuminate(form = f, shadow = s)
      }, promo = promotion)
    }

  }
}
