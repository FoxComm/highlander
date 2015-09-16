package payloads

import models.GiftCard
import utils.Money._

final case class GiftCardCreateByCsrPayload(balance: Int, currency: Currency = Currency.USD)

final case class GiftCardUpdateStatusPayload(status: GiftCard.Status)