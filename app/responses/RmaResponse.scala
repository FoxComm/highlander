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
      customer = customer,
      storeAdmin = storeAdmin,
      lineItems = mockLineItems,
      payment = Some(mockPayment))
  }

  private def fetchRmaDetails(rma: Rma)(implicit ec: ExecutionContext) = {
    for {
      customer ← rma.customerId match {
        case Some(id) ⇒ Customers.findById(id).extract.one
        case None     ⇒ lift(None)
      }

      storeAdmin ← rma.storeAdminId match {
        case Some(id) ⇒ StoreAdmins.findById(id).extract.one
        case None     ⇒ lift(None)
      }

    } yield (customer, storeAdmin)
  }
}