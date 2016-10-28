package payloads

import models.promotion.Promotion.ApplyType
import payloads.DiscountPayloads.CreateDiscount
import utils.aliases._

object PromotionPayloads {

  case class UpdatePromoDiscount(id: Int, attributes: Map[String, Json])

  case class CreatePromotion(applyType: ApplyType,
                             attributes: Map[String, Json],
                             discounts: Seq[CreateDiscount],
                             schema: Option[String] = None,
                             scope: Option[String] = None)

  case class UpdatePromotion(applyType: ApplyType,
                             attributes: Map[String, Json],
                             discounts: Seq[UpdatePromoDiscount])
}
