package responses

import models._

object RmaResponse {
  final case class Root(
    id: Int,
    referenceNumber: String,
    orderId: Int,
    rmaType: Rma.RmaType,
    status: Rma.Status,
    lineItems: LineItems,
    customer: Option[Customer],
    storeAdmin: Option[StoreAdmin],
    payment: Option[FullOrder.DisplayPayment] = None) extends ResponseItem

  final case class LineItems(
    skus: Seq[FullOrder.DisplayLineItem] = Seq.empty,
    giftCards: Seq[GiftCardResponse.Root] = Seq.empty,
    shippingCosts: Seq[ShipmentResponse.Root] = Seq.empty) extends ResponseItem

  def build(rma: Rma, customer: Option[Customer] = None, storeAdmin: Option[StoreAdmin] = None): Root = {
    Root(id = rma.id,
      referenceNumber = rma.refNum,
      orderId = rma.orderId,
      rmaType = rma.rmaType,
      status = rma.status,
      lineItems = LineItems(),
      customer = customer,
      storeAdmin = storeAdmin)
  }
}