package responses

import io.circe.syntax._
import models.shipping.ShippingMethod
import utils.aliases._
import utils.json.codecs._

object ShippingMethodsResponse {
  case class Root(id: Int, name: String, code: String, price: Int, isEnabled: Boolean)
      extends ResponseItem {
    def json: Json = this.asJson
  }

  def build(record: ShippingMethod, isEnabled: Boolean = true): Root =
    Root(id = record.id,
         name = record.adminDisplayName,
         code = record.code,
         price = record.price,
         isEnabled = isEnabled)
}
