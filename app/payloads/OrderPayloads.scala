package payloads

import models.Order.Status

case class UpdateOrderPayload(status: Status)

case class CreateOrderNotePayload(body: String)
