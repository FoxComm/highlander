package models.cord

import payloads.AddressPayloads.CreateAddressPayload

package object lineitems {

  case class LineItemAttributes(giftCard: Option[GiftCardLineItemAttributes] = None,
                                subscription: Option[CreateAddressPayload] = None)

  /*
   * GC line item attrs receive `code` when all of the following fulfill:
   * 1. cart becomes order
   * 2. order moves to Shipped
   * 3. MWH creates gift cards
   * Hence, order has no `code` for some period of time, so we can't guarantee presence of `code`.
   */
  case class GiftCardLineItemAttributes(senderName: String,
                                        recipientName: String,
                                        recipientEmail: String,
                                        message: String,
                                        code: Option[String] = None)

}
