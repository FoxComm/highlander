package payloads

object PricePayloads {
  case class PricePayload(currency: String, value: Int)
}
