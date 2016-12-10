package payloads

object ShippingMethodPayloadsPayloads {
  case class CreateShippingMethodPayload(adminDisplayName: String,
                                         storefrontDisplayName: String,
                                         code: String,
                                         price: Int)

  case class UpdateShippingMethodPayload(adminDisplayName: Option[String],
                                         storefrontDisplayName: Option[String],
                                         price: Option[Int])
}
