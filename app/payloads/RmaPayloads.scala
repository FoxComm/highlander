package payloads

import models.Rma

final case class RmaCreatePayload(orderId: Int, orderRefNum: String, rmaType: Rma.RmaType)

final case class RmaUpdateStatusPayload(status: Rma.Status)