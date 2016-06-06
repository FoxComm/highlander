package payloads

import org.json4s.JsonDSL._
import utils.aliases._

object ObjectPayloads {
  case class StringAttributePayload(name: String, value: String) {
    val formJson: Json   = name → value
    val shadowJson: Json = name → (("type" → "string") ~ ("ref" → name))
  }
}
