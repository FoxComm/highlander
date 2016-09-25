package payloads

import utils.aliases._

object DiscountPayloads {

  case class CreateDiscount(attributes: Map[String, Json])

  case class UpdateDiscount(attributes: Map[String, Json])
}
