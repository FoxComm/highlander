package utils

import java.time.Instant

import models.discount.offers.{Offer, OfferType}
import models.discount.qualifiers.{Qualifier, QualifierType}
import models.objects.ObjectUtils
import models.promotion.Promotion
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization._
import utils.JsonFormatters.phoenixFormats

package object seeds {
  case class BaseDiscount(title: String, discountId: Int = 0, formId: Int = 0, shadowId: Int = 0)

  case class BaseDiscountForm(title: String, qualifier: Qualifier, offer: Offer) {

    implicit val formats = phoenixFormats

    val qualifierType = QualifierType.show(qualifier.qualifierType)
    val offerType     = OfferType.show(offer.offerType)

    val qualifierJson = s"""{"$qualifierType": ${write(qualifier)}}"""
    val offerJson     = s"""{"$offerType": ${write(offer)}}"""

    val (keyMap, form) = ObjectUtils.createForm(parse(s"""
    {
      "title" : "$title",
      "description" : "$title",
      "tags" : [],
      "qualifier": $qualifierJson,
      "offer" : $offerJson
    }"""))
  }

  case class BaseDiscountShadow(f: BaseDiscountForm) {

    val shadow = ObjectUtils.newShadow(
      parse("""
        {
          "title" : {"type": "string", "ref": "title"},
          "description" : {"type": "richText", "ref": "description"},
          "tags" : {"type": "tags", "ref": "tags"},
          "qualifier" : {"type": "qualifier", "ref": "qualifier"},
          "offer" : {"type": "offer", "ref": "offer"}
        }"""),
      f.keyMap)
  }

  case class BasePromotion(promotionId: Int = 0,
                           formId: Int = 0,
                           shadowId: Int = 0,
                           applyType: Promotion.ApplyType,
                           title: String)

  case class BasePromotionForm(name: String, applyType: Promotion.ApplyType) {

    val (keyMap, form) = ObjectUtils.createForm(parse(s"""
    {
      "name" : "$name",
      "storefrontName" : "$name",
      "description" : "$name",
      "details" : "",
      "activeFrom" : "${Instant.now}",
      "activeTo" : null,
      "tags" : []
      }
    }"""))
  }

  case class BasePromotionShadow(f: BasePromotionForm) {

    val shadow = ObjectUtils.newShadow(
      parse("""
        {
          "name" : {"type": "string", "ref": "name"},
          "storefrontName" : {"type": "richText", "ref": "storefrontName"},
          "description" : {"type": "text", "ref": "description"},
          "details" : {"type": "richText", "ref": "details"},
          "activeFrom" : {"type": "date", "ref": "activeFrom"},
          "activeTo" : {"type": "date", "ref": "activeTo"},
          "tags" : {"type": "tags", "ref": "tags"}
        }"""),
      f.keyMap)
  }

  case class BaseCoupon(formId: Int = 0, shadowId: Int = 0, promotionId: Int)

  case class BaseCouponForm(title: String) {

    val (keyMap, form) = ObjectUtils.createForm(parse(s"""
    {
      "name" : "$title",
      "storefrontName" : "$title",
      "description" : "$title",
      "details" : "",
      "activeFrom" : "${Instant.now}",
      "activeTo" : null,
      "tags" : [],
      "usageRules": {
        "isExclusive": false,
        "isUnlimitedPerCode": true,
        "isUnlimitedPerCustomer": true
      }
    }"""))
  }

  case class BaseCouponShadow(f: BaseCouponForm) {

    val shadow = ObjectUtils.newShadow(
      parse("""
        {
          "name" : {"type": "string", "ref": "name"},
          "storefrontName" : {"type": "richText", "ref": "storefrontName"},
          "description" : {"type": "text", "ref": "description"},
          "details" : {"type": "richText", "ref": "details"},
          "activeFrom" : {"type": "date", "ref": "activeFrom"},
          "activeTo" : {"type": "date", "ref": "activeTo"},
          "tags" : {"type": "tags", "ref": "tags"},
          "usageRules" : {"type": "usageRules", "ref": "usageRules"}
        }"""),
      f.keyMap)
  }
}
