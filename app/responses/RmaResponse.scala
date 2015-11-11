package responses

import java.time.Instant

import scala.concurrent.ExecutionContext

import models._
import payloads._
import responses.FullOrder.DisplayLineItem
import responses.CustomerResponse.{Root ⇒ Customer}
import responses.StoreAdminResponse.{Root ⇒ StoreAdmin}
import services.NotFoundFailure404

import slick.driver.PostgresDriver.api._
import utils.Money._
import utils.Slick._
import utils.Slick.implicits._

object RmaResponse {
  val mockTotal = RmaTotals(subTotal = 70, shipping = 10, taxes = 10, total = 50)

  val mockLineItems = LineItems(
    skus = Seq(
      DisplayLineItem(sku = "SKU-YAX", status = OrderLineItem.Shipped),
      DisplayLineItem(sku = "SKU-ABC", status = OrderLineItem.Shipped),
      DisplayLineItem(sku = "SKU-ZYA", status = OrderLineItem.Shipped)
    )
  )

  final case class RmaTotals(subTotal: Int, shipping: Int, taxes: Int, total: Int) extends ResponseItem

  final case class FullRmaWithWarnings(rma: Root, warnings: Seq[NotFoundFailure404])
  final case class FullRmaExpandedWithWarnings(rma: RootExpanded, warnings: Seq[NotFoundFailure404])

  final case class Root(
    id: Int,
    referenceNumber: String,
    orderRefNum: String,
    rmaType: Rma.RmaType,
    status: Rma.Status,
    lineItems: LineItems = LineItems(),
    payments: Seq[DisplayPayment] = Seq.empty,
    customer: Option[Customer] = None,
    storeAdmin: Option[StoreAdmin] = None,
    assignees: Seq[AssignmentResponse.Root],
    createdAt: Instant,
    updatedAt: Instant,
    totals: RmaTotals) extends ResponseItem

  final case class RootExpanded(
    id: Int,
    referenceNumber: String,
    order: Option[FullOrder.Root],
    rmaType: Rma.RmaType,
    status: Rma.Status,
    lineItems: LineItems = LineItems(),
    payments: Seq[DisplayPayment] = Seq.empty,
    customer: Option[Customer] = None,
    storeAdmin: Option[StoreAdmin] = None,
    assignees: Seq[AssignmentResponse.Root],
    createdAt: Instant,
    updatedAt: Instant,
    totals: RmaTotals) extends ResponseItem

  final case class LineItems(
    skus: Seq[FullOrder.DisplayLineItem] = Seq.empty,
    giftCards: Seq[GiftCardResponse.Root] = Seq.empty,
    shippingCosts: Seq[ShipmentResponse.Root] = Seq.empty) extends ResponseItem

  final case class DisplayPayment(
    id: Int,
    amount: Int,
    currency: Currency = Currency.USD,
    paymentMethodId: Int,
    paymentMethodType: PaymentMethod.Type) extends ResponseItem

  def buildPayment(pmt: RmaPayment): DisplayPayment =
    DisplayPayment(
      id = pmt.id,
      amount = pmt.amount,
      currency = pmt.currency,
      paymentMethodId = pmt.paymentMethodId,
      paymentMethodType = pmt.paymentMethodType
    )

  def fromRma(rma: Rma)(implicit ec: ExecutionContext, db: Database): DBIO[Root] = {
    fetchRmaDetails(rma).map { case (customer, storeAdmin, assignments, payments) ⇒
      build(
        rma = rma,
        customer = customer.map(CustomerResponse.build(_)),
        storeAdmin = storeAdmin.map(StoreAdminResponse.build),
        payments = payments.map(buildPayment),
        assignees = assignments.map((AssignmentResponse.buildForRma _).tupled)
      )
    }
  }

  def fromRmaExpanded(rma: Rma)(implicit ec: ExecutionContext, db: Database): DBIO[RootExpanded] = {
    fetchRmaDetailsExpanded(rma).map { case (customer, storeAdmin, fullOrder, assignments, payments) ⇒
      buildExpanded(
        rma = rma,
        order = fullOrder,
        customer = customer.map(CustomerResponse.build(_)),
        storeAdmin = storeAdmin.map(StoreAdminResponse.build),
        payments = payments.map(buildPayment),
        assignees = assignments.map((AssignmentResponse.buildForRma _).tupled)
      )
    }
  }

  def buildMockRma(id: Int, refNum: String, admin: Option[StoreAdmin] = None, customer: Option[Customer] = None): Root =
    Root(
      id = id,
      referenceNumber = refNum,
      orderRefNum = "ABC-123",
      rmaType = Rma.Standard,
      status = Rma.Pending,
      customer = customer,
      storeAdmin = admin,
      lineItems = mockLineItems,
      assignees = Seq.empty,
      createdAt = Instant.now,
      updatedAt = Instant.now,
      totals = mockTotal)

  def build(rma: Rma, customer: Option[Customer] = None, storeAdmin: Option[StoreAdmin] = None,
    payments: Seq[DisplayPayment] = Seq.empty, assignees: Seq[AssignmentResponse.Root] = Seq.empty): Root = {
    Root(id = rma.id,
      referenceNumber = rma.refNum,
      orderRefNum = rma.orderRefNum,
      rmaType = rma.rmaType,
      status = rma.status,
      customer = customer,
      storeAdmin = storeAdmin,
      payments = payments,
      assignees = assignees,
      createdAt = rma.createdAt,
      updatedAt = rma.updatedAt,
      totals = mockTotal)
  }

  def buildExpanded(rma: Rma, order: Option[FullOrder.Root] = None, customer: Option[Customer] = None,
    storeAdmin: Option[StoreAdmin] = None, payments: Seq[DisplayPayment] = Seq.empty,
    assignees: Seq[AssignmentResponse.Root] = Seq.empty): RootExpanded = {
    RootExpanded(id = rma.id,
      referenceNumber = rma.refNum,
      order = order,
      rmaType = rma.rmaType,
      status = rma.status,
      customer = customer,
      storeAdmin = storeAdmin,
      payments = payments,
      assignees = assignees,
      createdAt = rma.createdAt,
      updatedAt = rma.updatedAt,
      totals = mockTotal)
  }

  private def fetchRmaDetails(rma: Rma)(implicit ec: ExecutionContext, db: Database) = {
    for {
      customer ← Customers.findById(rma.customerId).extract.one
      storeAdmin ← rma.storeAdminId.map(id ⇒ StoreAdmins.findById(id).extract.one).getOrElse(lift(None))

      assignments ← RmaAssignments.filter(_.rmaId === rma.id).result
      admins ← StoreAdmins.filter(_.id.inSetBind(assignments.map(_.assigneeId))).result

      payments ← RmaPayments.filter(_.rmaId === rma.id).result
    } yield (customer, storeAdmin, assignments.zip(admins), payments)
  }

  private def fetchRmaDetailsExpanded(rma: Rma)(implicit ec: ExecutionContext, db: Database) = {
    for {
      order ← Orders.findById(rma.orderId).extract.one
      fullOrder ← order.map(o ⇒ FullOrder.fromOrder(o).map(Some(_))).getOrElse(lift(None))

      customer ← Customers.findById(rma.customerId).extract.one
      storeAdmin ← rma.storeAdminId.map(id ⇒ StoreAdmins.findById(id).extract.one).getOrElse(lift(None))

      assignments ← RmaAssignments.filter(_.rmaId === rma.id).result
      admins ← StoreAdmins.filter(_.id.inSetBind(assignments.map(_.assigneeId))).result

      payments ← RmaPayments.filter(_.rmaId === rma.id).result
    } yield (customer, storeAdmin, fullOrder, assignments.zip(admins), payments)
  }
}