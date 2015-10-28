package responses

import models.Shipment
import models.Shipment._

object ShipmentResponse {
  final case class Root(id: Int, orderId: Int, status: Status, shippingPrice: Option[Int] = None) extends ResponseItem

  def build(shipment: Shipment): Root =
    Root(id = shipment.id, orderId = shipment.orderId, status = shipment.status, shippingPrice = shipment.shippingPrice)
}
