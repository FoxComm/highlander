package payloads

final case class RmaCreatePayload(orderId: Int, rmaType: String)

final case class RmaUpdateStatusPayload(status: String, reasonId: Int)