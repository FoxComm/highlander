package responses

import scala.concurrent.ExecutionContext

import models._
import responses.FullOrder.{DisplayLineItem, DisplayPayment, DisplayPaymentMethod}
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

  val mockPayment = DisplayPayment(
    amount = 256,
    status = CreditCardCharge.Auth,
    referenceNumber = "ABC-123",
    paymentMethod = DisplayPaymentMethod(
      cardType = "visa",
      cardNumber = "5555-5555-5555-555",
      cardExp = "05/15"
    )
  )

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
    payment: Option[DisplayPayment] = None) extends ResponseItem

  final case class RootExpanded(
    id: Int,
    referenceNumber: String,
    order: Option[FullOrder.Root],
    rmaType: Rma.RmaType,
    status: Rma.Status,
    lineItems: LineItems,
    customer: Option[Customer] = None,
    storeAdmin: Option[StoreAdmin] = None,
    payment: Option[DisplayPayment] = None) extends ResponseItem

  final case class LineItems(
    skus: Seq[FullOrder.DisplayLineItem] = Seq.empty,
    giftCards: Seq[GiftCardResponse.Root] = Seq.empty,
    shippingCosts: Seq[ShipmentResponse.Root] = Seq.empty) extends ResponseItem

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
      payment = Some(mockPayment))

  def build(rma: Rma, customer: Option[Customer] = None, storeAdmin: Option[StoreAdmin] = None): Root = {
    Root(id = rma.id,
      referenceNumber = rma.refNum,
      orderId = rma.orderId,
      orderRefNum = rma.orderRefNum,
      rmaType = rma.rmaType,
      status = rma.status,
      customer = customer,
      storeAdmin = storeAdmin,
      lineItems = mockLineItems,
      payment = None)
  }

  def buildExpanded(rma: Rma, order: Option[FullOrder.Root] = None,
    customer: Option[Customer] = None, storeAdmin: Option[StoreAdmin] = None): RootExpanded = {
    RootExpanded(id = rma.id,
      referenceNumber = rma.refNum,
      order = order,
      rmaType = rma.rmaType,
      status = rma.status,
      customer = customer,
      storeAdmin = storeAdmin,
      lineItems = mockLineItems,
      payment = None)
  }

  private def fetchRmaDetails(rma: Rma)(implicit ec: ExecutionContext, db: Database) = {
    for {
      customer ← rma.customerId match {
        case Some(id) ⇒ Customers.findById(id).extract.one
        case _        ⇒ lift(None)
      }

      storeAdmin ← rma.storeAdminId match {
        case Some(id) ⇒ StoreAdmins.findById(id).extract.one
        case _        ⇒ lift(None)
      }
    } yield (customer, storeAdmin)
  }

  private def fetchRmaDetailsExpanded(rma: Rma)(implicit ec: ExecutionContext, db: Database) = {
    for {
      order ← Orders.findById(rma.orderId).extract.one

      fullOrder ← order match {
        case Some(o) ⇒ FullOrder.fromOrder(o).map(Some(_))
        case _       ⇒ lift(None)
      }

      customer ← rma.customerId match {
        case Some(id) ⇒ Customers.findById(id).extract.one
        case _        ⇒ lift(None)
      }

      storeAdmin ← rma.storeAdminId match {
        case Some(id) ⇒ StoreAdmins.findById(id).extract.one
        case _        ⇒ lift(None)
      }
    } yield (customer, storeAdmin, fullOrder)
  }
}