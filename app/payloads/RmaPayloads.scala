package payloads

import models.Rma

final case class RmaCreatePayload(orderId: Int, orderRefNum: String, rmaType: Rma.RmaType)

final case class RmaUpdateStatusPayload(status: Rma.Status)

final case class RmaUpdateSkuLineItemsPayload(sku: String, quantity: Int, reasonId: Int)

final case class RmaUpdateGiftCardLineItemsPayload(code: String, reasonId: Int)

final case class RmaUpdateShippingCostLineItemsPayload(id: Int, reasonId: Int)