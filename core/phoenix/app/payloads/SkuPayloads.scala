package payloads

import utils.aliases._

object SkuPayloads {
  case class SkuPayload(attributes: Map[String, Json])
}
