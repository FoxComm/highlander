package payloads

import models.Rma

final case class RmaCreatePayload(orderId: Int, orderRefNum: String, rmaType: Rma.RmaType)

final case class RmaUpdateStatusPayload(status: Rma.Status)

/* Line item updater payloads */

final case class RmaSkuLineItemsPayload(sku: String, quantity: Int, reasonId: Int)

final case class RmaGiftCardLineItemsPayload(code: String, reasonId: Int)

final case class RmaShippingCostLineItemsPayload(id: Int, reasonId: Int)

/* Payment payloads */

final case class RmaCreditCardPayment(id: Int, amount: Int)

final case class RmaGiftCardPayment(amount: Int)

final case class RmaStoreCreditPayment(amount: Int)

/* Assignees */

final case class RmaAssigneesPayload(assignees: Seq[Int])

final case class RmaBulkAssigneesPayload(referenceNumbers: Seq[String], assigneeId: Int)