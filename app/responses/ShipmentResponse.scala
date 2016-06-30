package responses

import models.shipping.Shipment
import models.shipping.Shipment._

object ShipmentResponse {
  case class Root(id: Int, orderRef: String, state: State, shippingPrice: Option[Int] = None)
      extends ResponseItem

  def build(shipment: Shipment): Root =
    Root(id = shipment.id,
         orderRef = shipment.orderRef,
         state = shipment.state,
         shippingPrice = shipment.shippingPrice)
}
