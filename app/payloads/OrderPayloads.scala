package payloads

import models.Order.Status

final case class UpdateOrderPayload(status: Status)

final case class CreateOrderNotePayload(body: String)
