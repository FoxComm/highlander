package responses

import models.shipping.ShippingMethod

object ShippingMethodsResponse {
  case class Root(id: Int, name: String, code: String, price: Int, isEnabled: Boolean)
      extends ResponseItem

  def build(record: ShippingMethod, isEnabled: Boolean = true): Root =
    Root(id = record.id,
         name = record.adminDisplayName,
         code = record.code,
         price = record.price,
         isEnabled = isEnabled)
}

object AdminShippingMethodsResponse {
  case class Root(id: Int,
                  adminDisplayName: String,
                  storefrontDisplayName: String,
                  code: String,
                  price: Int,
                  isActive: Boolean)

  def build(record: ShippingMethod): Root =
    Root(id = record.id,
         adminDisplayName = record.adminDisplayName,
         storefrontDisplayName = record.storefrontDisplayName,
         code = record.code,
         price = record.price,
         isActive = record.isActive)
}
