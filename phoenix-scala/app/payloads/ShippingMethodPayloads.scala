package payloads

import models.rules.QueryStatement
import payloads.PricePayloads._

object ShippingMethodPayloadsPayloads {
  case class CreateShippingMethodPayload(name: String,
                                         code: String,
                                         price: PricePayload,
                                         eta: Option[String] = None,
                                         carrier: Option[String] = None,
                                         conditions: Option[QueryStatement] = None,
                                         restrictions: Option[QueryStatement] = None)

  case class UpdateShippingMethodPayload(name: Option[String] = None,
                                         price: Option[PricePayload] = None,
                                         eta: Option[String] = None,
                                         carrier: Option[String] = None,
                                         conditions: Option[QueryStatement] = None,
                                         restrictions: Option[QueryStatement] = None)
}
