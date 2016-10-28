package payloads

import utils.aliases._

object DiscountPayloads {

  case class CreateDiscount(attributes: Map[String, Json],
                            schema: Option[String] = None,
                            scope: Option[String] = None)

  case class UpdateDiscount(attributes: Map[String, Json])
}
