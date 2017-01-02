package responses

import models.shipping.ShippingMethod

object ShippingMethodsResponse {
  case class Root(id: Int, name: String, code: String, price: Int, eta: String, isEnabled: Boolean)
      extends ResponseItem

  def build(record: ShippingMethod, isEnabled: Boolean = true): Root =
    Root(id = record.id,
         name = record.name,
         code = record.code,
         price = record.price,
         eta = record.eta.getOrElse(""),
         isEnabled = isEnabled)
}

object AdminShippingMethodsResponse {
  case class Root(id: Int,
                  name: String,
                  code: String,
                  price: PriceResponse.Root,
                  eta: String,
                  carrier: String,
                  isActive: Boolean)

  def build(record: ShippingMethod): Root =
    Root(id = record.id,
         name = record.name,
         code = record.code,
         price = PriceResponse.build(record.price),
         eta = record.eta.getOrElse(""),
         carrier = record.carrier.getOrElse(""),
         isActive = record.isActive)
}
