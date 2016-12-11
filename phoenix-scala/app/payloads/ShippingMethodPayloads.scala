package payloads

object ShippingMethodPayloadsPayloads {
  case class CreateShippingMethodPayload(adminDisplayName: String,
                                         storefrontDisplayName: String,
                                         code: String,
                                         price: Int)

  case class UpdateShippingMethodPayload(adminDisplayName: Option[String] = None,
                                         storefrontDisplayName: Option[String] = None,
                                         price: Option[Int] = None)
}
