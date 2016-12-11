package payloads

object ShippingMethodPayloadsPayloads {
  case class CreateShippingMethodPayload(adminDisplayName: String,
                                         storefrontDisplayName: String,
                                         code: String,
                                         price: Int,
                                         eta: Option[String] = None,
                                         carrier: Option[String] = None)

  case class UpdateShippingMethodPayload(adminDisplayName: Option[String] = None,
                                         storefrontDisplayName: Option[String] = None,
                                         price: Option[Int] = None,
                                         eta: Option[String] = None,
                                         carrier: Option[String] = None)
}
