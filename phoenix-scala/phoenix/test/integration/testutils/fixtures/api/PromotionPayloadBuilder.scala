package testutils.fixtures.api

import java.time.Instant

import org.json4s.JsonAST.{JArray, JNull}
import org.json4s.jackson.parseJson
import phoenix.models.promotion.Promotion.ApplyType
import phoenix.payloads.DiscountPayloads.CreateDiscount
import phoenix.payloads.PromotionPayloads.CreatePromotion
import phoenix.utils.aliases.Json
import testutils.PayloadHelpers._

object PromotionPayloadBuilder {

  def build(applyType: ApplyType,
            offer: PromoOfferBuilder,
            qualifier: PromoQualifierBuilder,
            tags: PromoTagsBuilder = PromoTagsBuilder.Empty,
            title: String = faker.Lorem.sentence(),
            extraAttrs: Map[String, Json] = Map.empty,
            description: String = faker.Lorem.sentence()): CreatePromotion = {

    val discountAttrs = Map[String, Json](
      "title"       → tv(title),
      "description" → tv(description),
      "tags"        → tags.payloadJson,
      "qualifier"   → qualifier.payloadJson,
      "offer"       → offer.payloadJson
    )

    CreatePromotion(
      applyType = applyType,
      attributes = Map(
        "name"       → tv(faker.Lorem.sentence(1)),
        "activeFrom" → tv(Instant.now, "datetime"),
        "activeTo"   → tv(JNull, "datetime")
      ) ++ extraAttrs,
      discounts = Seq(CreateDiscount(discountAttrs))
    )
  }

  sealed trait PromoQualifierBuilder extends Jsonable

  object PromoQualifierBuilder {

    case object CartAny extends PromoQualifierBuilder {
      def payloadJson: Json = tv(parseJson("""{ "orderAny": {} }"""), "qualifier")
    }

    case class CartTotalAmount(qualifiedSubtotal: Long) extends PromoQualifierBuilder {
      def payloadJson: Json =
        tv(parseJson(s"""{ "orderTotalAmount": { "totalAmount" : $qualifiedSubtotal } }"""), "qualifier")
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
