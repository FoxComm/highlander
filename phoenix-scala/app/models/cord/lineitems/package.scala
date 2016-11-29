package models.cord

import payloads.AddressPayloads.CreateAddressPayload

package object lineitems {

  case class LineItemAttributes(giftCard: Option[GiftCardLineItemAttributes] = None,
                                subscription: Option[CreateAddressPayload] = None)

  case class GiftCardLineItemAttributes(senderName: String,
                                        recipientName: String,
                                        recipientEmail: String)

}
