package payloads

case class WmsEventPayload(skuId: Int, warehouseId: Int, onHand: Int, onHold: Int, reserved: Int)
