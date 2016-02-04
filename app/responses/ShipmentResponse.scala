package responses

import models.Shipment
import models.Shipment._

object ShipmentResponse {
  final case class Root(id: Int, orderId: Int, state: State, shippingPrice: Option[Int] = None) extends ResponseItem

  def build(shipment: Shipment): Root =
    Root(id = shipment.id, orderId = shipment.orderId, state = shipment.state, shippingPrice = shipment.shippingPrice)
}
