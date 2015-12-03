package responses

import java.time.Instant

import models.{Customers, GiftCard, Orders, PaymentMethod, Rma, RmaAssignments, RmaLineItem, RmaLineItemGiftCards,
RmaLineItemShippingCosts, RmaLineItemSkus, RmaPayment, RmaPayments, Shipment, Sku, StoreAdmins}
import responses.CustomerResponse.{Root => Customer}
import responses.StoreAdminResponse.{Root => StoreAdmin}
import services.NotFoundFailure404
import services.rmas.RmaTotaler
import slick.driver.PostgresDriver.api._
import utils.Money._
import utils.Slick._
import utils.Slick.implicits._

import scala.concurrent.ExecutionContext

object RmaResponse {
  final case class RmaTotals(subTotal: Int, shipping: Int, taxes: Int, total: Int) extends ResponseItem
  final case class FullRmaWithWarnings(rma: Root, warnings: Seq[NotFoundFailure404])
  final case class FullRmaExpandedWithWarnings(rma: RootExpanded, warnings: Seq[NotFoundFailure404])

  final case class LineItemSku(lineItemId: Int, sku: DisplaySku) extends ResponseItem
  final case class LineItemGiftCard(lineItemId: Int, giftCard: GiftCardResponse.Root) extends ResponseItem
  final case class LineItemShippingCost(lineItemId: Int, shippingCost: ShipmentResponse.Root) extends ResponseItem

  final case class LineItems(
    skus: Seq[LineItemSku] = Seq.empty,
    giftCards: Seq[LineItemGiftCard] = Seq.empty,
    shippingCosts: Seq[LineItemShippingCost] = Seq.empty) extends ResponseItem

  final case class DisplayPayment(
    id: Int,
    amount: Int,
    currency: Currency = Currency.USD,
    paymentMethodId: Int,
    paymentMethodType: PaymentMethod.Type) extends ResponseItem

  final case class DisplaySku(
    imagePath: String = "http://lorempixel.com/75/75/fashion",
    name: String = "donkey product",
    sku: String,
    price: Int = 33,
    quantity: Int = 1,
    totalPrice: Int = 33) extends ResponseItem

  final case class Root(
    id: Int,
    referenceNumber: String,
    orderRefNum: String,
    rmaType: Rma.RmaType,
    status: Rma.Status,
    lineItems: LineItems,
    payments: Seq[DisplayPayment],
    customer: Option[Customer] = None,
    storeAdmin: Option[StoreAdmin] = None,
    assignees: Seq[AssignmentResponse.Root],
    messageToCustomer: Option[String] = None,
    canceledReason: Option[Int] = None,
    createdAt: Instant,
    updatedAt: Instant,
    totals: Option[RmaTotals]) extends ResponseItem

  final case class RootExpanded(
    id: Int,
    referenceNumber: String,
    order: Option[FullOrder.Root],
    rmaType: Rma.RmaType,
    status: Rma.Status,
    lineItems: LineItems,
    payments: Seq[DisplayPayment],
    customer: Option[Customer] = None,
    storeAdmin: Option[StoreAdmin] = None,
    assignees: Seq[AssignmentResponse.Root],
    messageToCustomer: Option[String] = None,
    canceledReason: Option[Int] = None,
    createdAt: Instant,
    updatedAt: Instant,
    totals: Option[RmaTotals]) extends ResponseItem

  def buildPayment(pmt: RmaPayment): DisplayPayment =
    DisplayPayment(
      id = pmt.id,
      amount = pmt.amount,
      currency = pmt.currency,
      paymentMethodId = pmt.paymentMethodId,
      paymentMethodType = pmt.paymentMethodType
    )

  def buildLineItems(skus: Seq[(Sku, RmaLineItem)], giftCards: Seq[(GiftCard, RmaLineItem)],
    shipments: Seq[(Shipment, RmaLineItem)]): LineItems = {
    LineItems(
      skus = skus.map { case (sku, li) ⇒ LineItemSku(lineItemId = li.id, sku = DisplaySku(sku = sku.sku)) },
      giftCards = giftCards.map { case (gc, li) ⇒
        LineItemGiftCard(lineItemId = li.id, giftCard = GiftCardResponse.build(gc)) },
      shippingCosts = shipments.map { case (shipment, li) ⇒
        LineItemShippingCost(lineItemId = li.id, shippingCost = ShipmentResponse.build(shipment)) }
    )
  }

  def buildTotals(subtotal: Option[Int], taxes: Option[Int], shipments: Seq[(Shipment, RmaLineItem)]): RmaTotals = {
    val finalSubtotal = subtotal.getOrElse(0)
    val finalTaxes = taxes.getOrElse(0)
    val finalShipping = shipments.foldLeft(0){ case (acc, (shipment, li)) ⇒ acc + shipment.shippingPrice.getOrElse(0) }
    val grandTotal = finalSubtotal + finalShipping + finalTaxes
    RmaTotals(finalSubtotal, finalTaxes, finalShipping, grandTotal)
  }

  def fromRma(rma: Rma)(implicit ec: ExecutionContext, db: Database): DBIO[Root] = {
    fetchRmaDetails(rma).map {
      case (_, customer, storeAdmin, assignments, payments, skus, giftCards, shipments, subtotal) ⇒
        build(
          rma = rma,
          customer = customer.map(CustomerResponse.build(_)),
          storeAdmin = storeAdmin.map(StoreAdminResponse.build),
          payments = payments.map(buildPayment),
          assignees = assignments.map((AssignmentResponse.buildForRma _).tupled),
          lineItems = buildLineItems(skus, giftCards, shipments),
          totals = Some(buildTotals(subtotal, None, shipments))
        )
    }
  }

  def fromRmaExpanded(rma: Rma)(implicit ec: ExecutionContext, db: Database): DBIO[RootExpanded] = {
    fetchRmaDetails(rma = rma, withOrder = true).map {
      case (order, customer, storeAdmin, assignments, payments, skus, giftCards, shipments, subtotal) ⇒
        buildExpanded(
          rma = rma,
          order = order,
          customer = customer.map(CustomerResponse.build(_)),
          storeAdmin = storeAdmin.map(StoreAdminResponse.build),
          payments = payments.map(buildPayment),
          assignees = assignments.map((AssignmentResponse.buildForRma _).tupled),
          lineItems = buildLineItems(skus, giftCards, shipments),
          totals = Some(buildTotals(subtotal, None, shipments))
        )
    }
  }

  def build(rma: Rma, customer: Option[Customer] = None, storeAdmin: Option[StoreAdmin] = None,
    lineItems: LineItems = LineItems(), payments: Seq[DisplayPayment] = Seq.empty,
    assignees: Seq[AssignmentResponse.Root] = Seq.empty, totals: Option[RmaTotals] = None): Root = {
    Root(id = rma.id,
      referenceNumber = rma.refNum,
      orderRefNum = rma.orderRefNum,
      rmaType = rma.rmaType,
      status = rma.status,
      customer = customer,
      storeAdmin = storeAdmin,
      payments = payments,
      lineItems = lineItems,
      assignees = assignees,
      messageToCustomer = rma.messageToCustomer,
      canceledReason = rma.canceledReason,
      createdAt = rma.createdAt,
      updatedAt = rma.updatedAt,
      totals = totals)
  }

  def buildExpanded(rma: Rma, order: Option[FullOrder.Root] = None, customer: Option[Customer] = None,
    lineItems: LineItems = LineItems(), storeAdmin: Option[StoreAdmin] = None,
    payments: Seq[DisplayPayment] = Seq.empty, assignees: Seq[AssignmentResponse.Root] = Seq.empty,
    totals: Option[RmaTotals] = None): RootExpanded = {
    RootExpanded(id = rma.id,
      referenceNumber = rma.refNum,
      order = order,
      rmaType = rma.rmaType,
      status = rma.status,
      customer = customer,
      storeAdmin = storeAdmin,
      payments = payments,
      lineItems = lineItems,
      assignees = assignees,
      messageToCustomer = rma.messageToCustomer,
      canceledReason = rma.canceledReason,
      createdAt = rma.createdAt,
      updatedAt = rma.updatedAt,
      totals = totals)
  }

  private def fetchRmaDetails(rma: Rma, withOrder: Boolean = false)(implicit ec: ExecutionContext, db: Database) = {
    val orderQ = for {
      order     ← Orders.findById(rma.orderId).extract.one
      fullOrder ← order.map(o ⇒ FullOrder.fromOrder(o).map(Some(_))).getOrElse(lift(None))
    } yield fullOrder

    val orderDbio = if (withOrder) orderQ else lift(None)

    for {
      // Order, if necessary
      fullOrder   ← orderDbio
      // Either customer or storeAdmin as creator
      customer    ← Customers.findById(rma.customerId).extract.one
      storeAdmin  ← rma.storeAdminId.map(id ⇒ StoreAdmins.findById(id).extract.one).getOrElse(lift(None))
      // Assignments and related store admins
      assignments ← RmaAssignments.filter(_.rmaId === rma.id).result
      admins      ← StoreAdmins.filter(_.id.inSetBind(assignments.map(_.assigneeId))).result
      // Payment methods
      payments    ← RmaPayments.filter(_.rmaId === rma.id).result
      // Line items of each subtype
      skus        ← RmaLineItemSkus.findLineItemsByRma(rma).result
      giftCards   ← RmaLineItemGiftCards.findLineItemsByRma(rma).result
      shipments   ← RmaLineItemShippingCosts.findLineItemsByRma(rma).result
      // Subtotal
      subtotal    ← RmaTotaler.subTotal(rma)
    } yield (fullOrder, customer, storeAdmin, assignments.zip(admins), payments, skus, giftCards, shipments, subtotal)
  }
}