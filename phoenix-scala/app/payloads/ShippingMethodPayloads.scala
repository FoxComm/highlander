package payloads

import payloads.PricePayloads._

object ShippingMethodPayloadsPayloads {
  case class CreateShippingMethodPayload(adminDisplayName: String,
                                         storefrontDisplayName: String,
                                         code: String,
                                         price: PricePayload,
                                         eta: Option[String] = None,
                                         carrier: Option[String] = None)

  case class UpdateShippingMethodPayload(adminDisplayName: Option[String] = None,
                                         storefrontDisplayName: Option[String] = None,
                                         price: Option[PricePayload] = None,
                                         eta: Option[String] = None,
                                         carrier: Option[String] = None)
}
