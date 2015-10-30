package responses

import scala.concurrent.ExecutionContext

import models._
import payloads.{RmaCreditCardPayment, RmaGiftCardPayment, RmaStoreCreditPayment}
import responses.FullOrder.DisplayLineItem
import responses.CustomerResponse.{Root ⇒ Customer}
import responses.StoreAdminResponse.{Root ⇒ StoreAdmin}

import slick.driver.PostgresDriver.api._
import utils.Slick._
import utils.Slick.implicits._

object RmaResponse {
  val mockLineItems = LineItems(
    skus = Seq(
      DisplayLineItem(sku = "SKU-YAX", status = OrderLineItem.Shipped),
      DisplayLineItem(sku = "SKU-ABC", status = OrderLineItem.Shipped),
      DisplayLineItem(sku = "SKU-ZYA", status = OrderLineItem.Shipped)
    )
  )

  val mockPayments = Payments(
    creditCard = Some(RmaCreditCardPayment(id = 1, amount = 10)),
    giftCard = Some(RmaGiftCardPayment(amount = 10)),
    storeCredit = Some(RmaStoreCreditPayment(amount = 10))
  )

  final case class Root(
    id: Int,
    referenceNumber: String,
    orderId: Int,
    orderRefNum: String,
    rmaType: Rma.RmaType,
    status: Rma.Status,
    lineItems: LineItems = LineItems(),
    payments: Payments = Payments(),
    customer: Option[Customer] = None,
    storeAdmin: Option[StoreAdmin] = None) extends ResponseItem

  final case class RootExpanded(
    id: Int,
    referenceNumber: String,
    order: Option[FullOrder.Root],
    rmaType: Rma.RmaType,
    status: Rma.Status,
    lineItems: LineItems = LineItems(),
    payment: Payments = Payments(),
    customer: Option[Customer] = None,
    storeAdmin: Option[StoreAdmin] = None) extends ResponseItem

  final case class LineItems(
    skus: Seq[FullOrder.DisplayLineItem] = Seq.empty,
    giftCards: Seq[GiftCardResponse.Root] = Seq.empty,
    shippingCosts: Seq[ShipmentResponse.Root] = Seq.empty) extends ResponseItem

  final case class Payments(
    creditCard: Option[RmaCreditCardPayment] = None,
    giftCard: Option[RmaGiftCardPayment] = None,
    storeCredit: Option[RmaStoreCreditPayment] = None) extends ResponseItem

  def fromRma(rma: Rma)(implicit ec: ExecutionContext, db: Database): DBIO[Root] = {
    fetchRmaDetails(rma).map { case (customer, storeAdmin) ⇒
      build(
        rma = rma,
        customer = customer.map(CustomerResponse.build(_)),
        storeAdmin = storeAdmin.map(StoreAdminResponse.build)
      )
    }
  }

  def fromRmaExpanded(rma: Rma)(implicit ec: ExecutionContext, db: Database): DBIO[RootExpanded] = {
    fetchRmaDetailsExpanded(rma).map { case (customer, storeAdmin, fullOrder) ⇒
      buildExpanded(
        rma = rma,
        order = fullOrder,
        customer = customer.map(CustomerResponse.build(_)),
        storeAdmin = storeAdmin.map(StoreAdminResponse.build)
      )
    }
  }

  def buildMockRma(id: Int, refNum: String, orderId: Int, admin: Option[StoreAdmin] = None,
    customer: Option[Customer] = None): Root =
    Root(
      id = id,
      referenceNumber = refNum,
      orderId = orderId,
      orderRefNum = "ABC-123",
      rmaType = Rma.Standard,
      status = Rma.Pending,
      customer = customer,
      storeAdmin = admin,
      lineItems = mockLineItems,
      payments = mockPayments)

  def build(rma: Rma, customer: Option[Customer] = None, storeAdmin: Option[StoreAdmin] = None): Root = {
    Root(id = rma.id,
      referenceNumber = rma.refNum,
      orderId = rma.orderId,
      orderRefNum = rma.orderRefNum,
      rmaType = rma.rmaType,
      status = rma.status,
      customer = customer,
      storeAdmin = storeAdmin)
  }

  def buildExpanded(rma: Rma, order: Option[FullOrder.Root] = None,
    customer: Option[Customer] = None, storeAdmin: Option[StoreAdmin] = None): RootExpanded = {
    RootExpanded(id = rma.id,
      referenceNumber = rma.refNum,
      order = order,
      rmaType = rma.rmaType,
      status = rma.status,
      customer = customer,
      storeAdmin = storeAdmin)
  }

  private def fetchRmaDetails(rma: Rma)(implicit ec: ExecutionContext, db: Database) = {
    for {
      customer ← rma.customerId.map(id ⇒ Customers.findById(id).extract.one).getOrElse(lift(None))
      storeAdmin ← rma.customerId.map(id ⇒ StoreAdmins.findById(id).extract.one).getOrElse(lift(None))
    } yield (customer, storeAdmin)
  }

  private def fetchRmaDetailsExpanded(rma: Rma)(implicit ec: ExecutionContext, db: Database) = {
    for {
      order ← Orders.findById(rma.orderId).extract.one
      fullOrder ← order.map(o ⇒ FullOrder.fromOrder(o).map(Some(_))).getOrElse(lift(None))

      customer ← rma.customerId.map(id ⇒ Customers.findById(id).extract.one).getOrElse(lift(None))
      storeAdmin ← rma.customerId.map(id ⇒ StoreAdmins.findById(id).extract.one).getOrElse(lift(None))
    } yield (customer, storeAdmin, fullOrder)
  }
}