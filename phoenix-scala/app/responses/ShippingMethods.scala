package responses

import models.shipping.ShippingMethod

object ShippingMethods {
  case class Root(id: Int, name: String, price: Int, isEnabled: Boolean) extends ResponseItem

  def build(record: ShippingMethod, isEnabled: Boolean = true): Root =
    Root(id = record.id,
         name = record.adminDisplayName,
         price = record.price,
         isEnabled = isEnabled)
}
