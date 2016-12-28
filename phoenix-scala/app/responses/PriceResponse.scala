package responses

object PriceResponse {
  case class Root(currency: String, value: Int)

  def build(price: Int): Root =
    Root(currency = "USD", value = price)
}
