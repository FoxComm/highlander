package payloads

import payloads.PricePayloads._

object ShippingMethodPayloadsPayloads {
  case class CreateShippingMethodPayload(name: String,
                                         code: String,
                                         price: PricePayload,
                                         eta: Option[String] = None,
                                         carrier: Option[String] = None)

  case class UpdateShippingMethodPayload(name: Option[String] = None,
                                         price: Option[PricePayload] = None,
                                         eta: Option[String] = None,
                                         carrier: Option[String] = None)
}
