package responses

import models.shipping.Shipment
import Shipment._
import models.shipping.Shipment

object ShipmentResponse {
  final case class Root(id: Int, orderId: Int, state: State, shippingPrice: Option[Int] = None) extends ResponseItem

  def build(shipment: Shipment): Root =
    Root(id = shipment.id, orderId = shipment.orderId, state = shipment.state, shippingPrice = shipment.shippingPrice)
}
