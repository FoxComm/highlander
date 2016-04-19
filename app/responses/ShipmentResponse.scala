package responses

import models.shipping.Shipment
import models.shipping.Shipment._

object ShipmentResponse {
  case class Root(id: Int, orderId: Int, state: State, shippingPrice: Option[Int] = None) extends ResponseItem

  def build(shipment: Shipment): Root =
    Root(id = shipment.id, orderId = shipment.orderId, state = shipment.state, shippingPrice = shipment.shippingPrice)
}
