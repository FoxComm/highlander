package phoenix.responses

import phoenix.models.shipping.ShippingMethod

case class ShippingMethodsResponse(id: Int, name: String, code: String, price: Long, isEnabled: Boolean)
    extends ResponseItem

object ShippingMethodsResponse {

  def build(record: ShippingMethod, isEnabled: Boolean = true): ShippingMethodsResponse =
    ShippingMethodsResponse(id = record.id,
                            name = record.adminDisplayName,
                            code = record.code,
                            price = record.price,
                            isEnabled = isEnabled)
}
