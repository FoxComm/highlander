package payloads

import models.GiftCard

final case class GiftCardCreatePayload(balance: Int)

final case class GiftCardUpdateStatusPayload(status: GiftCard.Status)