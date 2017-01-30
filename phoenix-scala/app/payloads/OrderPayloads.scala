package payloads

import java.time.Instant

import models.cord.Order.State

object OrderPayloads {

  case class UpdateOrderPayload(state: State)

  case class BulkUpdateOrdersPayload(referenceNumbers: Seq[String], state: State)

  case class CreateOrderNotePayload(body: String)

  case class OrderTimeMachine(referenceNumber: String, placedAt: Instant)
}
