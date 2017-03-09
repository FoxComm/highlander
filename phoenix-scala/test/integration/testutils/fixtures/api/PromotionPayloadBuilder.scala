package testutils.fixtures.api

import models.promotion.Promotion.ApplyType
import org.json4s.JsonAST.JArray
import org.json4s.jackson.parseJson
import payloads.DiscountPayloads.CreateDiscount
import payloads.PromotionPayloads.CreatePromotion
import testutils.PayloadHelpers._
import utils.aliases.Json

object PromotionPayloadBuilder {

  def build(applyType: ApplyType,
            offer: PromoOfferBuilder,
            qualifier: PromoQualifierBuilder,
            tags: PromoTagsBuilder = PromoTagsBuilder.Empty,
            title: String = faker.Lorem.sentence(),
            description: String = faker.Lorem.sentence()): CreatePromotion = {

    val discountAttrs = Map[String, Json](
        "title"       → tv(title),
        "description" → tv(description),
        "tags"        → tags.payloadJson,
        "qualifier"   → qualifier.payloadJson,
        "offer"       → offer.payloadJson
    )

    CreatePromotion(applyType = applyType,
                    attributes = Map("name" → tv(faker.Lorem.sentence(1))),
                    discounts = Seq(CreateDiscount(discountAttrs)))
  }

  sealed trait PromoQualifierBuilder extends Jsonable

  object PromoQualifierBuilder {

    case class CartTotalAmount(qualifiedSubtotal: Int) extends PromoQualifierBuilder {
      def payloadJson: Json =
        tv(parseJson(s"""{ "orderTotalAmount": { "totalAmount" : $qualifiedSubtotal } }"""),
           "qualifier")
    }

    case class CartNumUnits(qualifiedNumUnits: Int) extends PromoQualifierBuilder {
      def payloadJson: Json =
        tv(parseJson(s"""{ "orderNumUnits": { "numUnits": $qualifiedNumUnits } }"""), "qualifier")
    }
  }

  sealed trait PromoOfferBuilder extends Jsonable

  object PromoOfferBuilder {

    case class CartPercentOff(percentOff: Int) extends PromoOfferBuilder {
      def payloadJson: Json =
        tv(parseJson(s"""{ "orderPercentOff" : { "discount" : $percentOff } }"""), "offer")
    }
  }

  sealed trait PromoTagsBuilder extends Jsonable

  object PromoTagsBuilder {

    case object Empty extends PromoTagsBuilder {
      def payloadJson: Json =
        tv(JArray(List.empty[Json]), "tags")
    }
  }

  trait Jsonable {
    def payloadJson: Json
  }
}
