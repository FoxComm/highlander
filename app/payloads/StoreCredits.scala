package payloads

import models.StoreCredit

final case class StoreCreditUpdateStatusByCsr(status: StoreCredit.Status, reason: Option[Int] = None)