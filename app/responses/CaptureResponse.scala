package responses

import java.time.Instant

import models.cord.Order
import models.location.Region
import utils.Money.Currency

object CaptureResponse {

  case class Root(order: String,
                  captured: Int,
                  external: Int,
                  internal: Int,
                  lineItems: Int,
                  taxes: Int,
                  shipping: Int,
                  currency: Currency)
      extends ResponseItem

  def build(order: Order,
            captured: Int,
            external: Int,
            internal: Int,
            lineItems: Int,
            taxes: Int,
            shipping: Int,
            currency: Currency) =
    Root(order = order.referenceNumber,
         captured = captured,
         external = external,
         internal = internal,
         lineItems = lineItems,
         taxes = taxes,
         shipping = shipping,
         currency = currency)
}
