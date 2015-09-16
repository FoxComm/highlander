package payloads

import models.GiftCard
import utils.Money._

final case class GiftCardCreateByCsr(balance: Int, currency: Currency = Currency.USD)

final case class GiftCardUpdateStatus(status: GiftCard.Status, reason: Option[String] = None)