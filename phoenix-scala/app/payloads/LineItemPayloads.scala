package payloads

import models.cord.lineitems._
import models.objects.ObjectForm

object LineItemPayloads {

  case class UpdateLineItemsPayload(productVariantId: ObjectForm#Id,
                                    quantity: Int,
                                    attributes: Option[LineItemAttributes] = None) {

    /** This alias is added so that comparisons between
      * `productVariantId`s and `formId`s don’t look weird. We’re
      * using form IDs to identify variants when communicating with
      * outside world. For more information, see the scaladoc for
      * [[models.inventory.ProductVariant]]. */
    val variantFormId: Int = productVariantId
  }

  case class UpdateOrderLineItemsPayload(
      state: OrderLineItem.State,
      attributes: Option[LineItemAttributes],
      referenceNumber: String
  )
}
