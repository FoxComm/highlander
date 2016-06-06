package payloads

import utils.aliases._

object VariantPayloads {
  case class CreateVariantPayload(attributes: Map[String, Json])
}
