package payloads

import models.GiftCard
import utils.Money._

final case class GiftCardCreateByCsr(balance: Int, currency: Currency = Currency.USD)

final case class GiftCardBulkCreateByCsr(count: Int, balance: Int, currency: Currency = Currency.USD)

final case class GiftCardUpdateStatusByCsr(status: GiftCard.Status, reason: Option[Int] = None)