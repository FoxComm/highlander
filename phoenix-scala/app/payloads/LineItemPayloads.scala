package payloads

import models.cord.lineitems._
import models.objects.ObjectForm

object LineItemPayloads {

  case class UpdateLineItemsPayload(productVariantId: ObjectForm#Id,
                                    quantity: Int,
                                    attributes: Option[LineItemAttributes] = None)

  case class UpdateOrderLineItemsPayload(
      state: OrderLineItem.State,
      attributes: Option[LineItemAttributes],
      referenceNumber: String
  )
}
