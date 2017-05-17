package payloads

import models.cord.lineitems._

object LineItemPayloads {

  case class UpdateLineItemsPayload(sku: String,
                                    quantity: Int,
                                    attributes: Option[LineItemAttributes] = None)

  case class UpdateOrderLineItemsPayload(
      state: OrderLineItem.State,
      attributes: Option[LineItemAttributes],
      referenceNumber: String
  )
}
