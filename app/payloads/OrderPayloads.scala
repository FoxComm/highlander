package payloads

case class UpdateOrderPayload(orderStatus: String) 

case class CreateOrderNotePayload(body: String)
