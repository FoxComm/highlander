package anthill.payloads

case class PurchaseEventPayload(customerId: Int, productIdList: List[Int], channel: Option[Int] = None)

