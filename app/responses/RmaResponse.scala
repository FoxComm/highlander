package responses

import models._
import responses.FullOrder.DisplayLineItem
import responses.CustomerResponse.{Root ⇒ Customer}
import responses.StoreAdminResponse.{Root ⇒ StoreAdmin}

object RmaResponse {
  final case class Root(
    id: Int,
    referenceNumber: String,
    orderId: Int,
    orderRefNum: String,
    rmaType: Rma.RmaType,
    status: Rma.Status,
    lineItems: LineItems,
    customer: Option[Customer] = None,
    storeAdmin: Option[StoreAdmin] = None,
    payment: Option[FullOrder.DisplayPayment] = None) extends ResponseItem

  final case class LineItems(
    skus: Seq[FullOrder.DisplayLineItem] = Seq.empty,
    giftCards: Seq[GiftCardResponse.Root] = Seq.empty,
    shippingCosts: Seq[ShipmentResponse.Root] = Seq.empty) extends ResponseItem

  def buildMockRma(id: Int, refNum: String, orderId: Int, admin: Option[StoreAdmin] = None,
    customer: Option[Customer] = None): Root =
    Root(
      id = id,
      referenceNumber = refNum,
      orderId = orderId,
      orderRefNum = "ABC-123",
      rmaType = Rma.Standard,
      status = Rma.Pending,
      lineItems = LineItems(
        skus = Seq(
          DisplayLineItem(sku = "SKU-YAX", status = OrderLineItem.Shipped),
          DisplayLineItem(sku = "SKU-ABC", status = OrderLineItem.Shipped),
          DisplayLineItem(sku = "SKU-ZYA", status = OrderLineItem.Shipped)
        )
      ),
      customer = customer,
      storeAdmin = admin)

  def buildMockSequence(admin: Option[StoreAdmin] = None, customer: Option[Customer] = None): Seq[Root] =
    Seq(
      buildMockRma(id = 1, refNum = "ABC-123", orderId = 1, admin = admin, customer = customer),
      buildMockRma(id = 2, refNum = "ABC-456", orderId = 1, admin = admin, customer = customer),
      buildMockRma(id = 3, refNum = "ABC-789", orderId = 1, admin = admin, customer = customer)
    )

  def build(rma: Rma, customer: Option[Customer] = None, storeAdmin: Option[StoreAdmin] = None): Root = {
    Root(id = rma.id,
      referenceNumber = rma.refNum,
      orderId = rma.orderId,
      orderRefNum = rma.orderRefNum,
      rmaType = rma.rmaType,
      status = rma.status,
      lineItems = LineItems(),
      customer = customer,
      storeAdmin = storeAdmin)
  }
}