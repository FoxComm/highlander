package phoenix.responses

import phoenix.models.shipping.ShippingMethod

object ShippingMethodsResponse {
  case class Root(id: Int, name: String, code: String, price: Long, isEnabled: Boolean) extends ResponseItem

  def build(record: ShippingMethod, isEnabled: Boolean = true): Root =
    Root(id = record.id,
         name = record.adminDisplayName,
         code = record.code,
         price = record.price,
         isEnabled = isEnabled)
}
