package responses

import java.time.Instant

import scala.concurrent.{ExecutionContext, Future}

import models._
import services._
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._

final case class FullOrderWithWarnings(order: FullOrder.Root, warnings: Seq[NotFoundFailure])

object FullOrder {
  type Response = Future[Root]

  final case class Totals(subTotal: Int, taxes: Int, adjustments: Int, total: Int)

  final case class Root(
    id: Int,
    referenceNumber: String,
    orderStatus: Order.Status,
    shippingStatus: Order.Status,
    paymentStatus: Order.Status,
    lineItems: Seq[DisplayLineItem],
    adjustments: Seq[Adjustment],
    fraudScore: Int,
    totals: Totals,
    customer: Option[Customer],
    shippingMethod: Option[ShippingMethod],
    shippingAddress: Option[OrderShippingAddress],
    assignees: Seq[AssignmentResponse.Root],
    remorsePeriodEnd: Option[Instant],
    payment: Option[DisplayPayment] = None
    )

  final case class DisplayLineItem(
    imagePath: String = "http://lorempixel.com/75/75/fashion",
    name: String = "donkey product",
    sku: String,
    price: Int = 33,
    quantity: Int = 1,
    totalPrice: Int = 33,
    status: OrderLineItem.Status
    )

  final case class DisplayPayment(amount: Int, status: String, referenceNumber: String = "ABC123", paymentMethod: DisplayPaymentMethod)
   //
  // TODO:
  // Capture reference number

  final case class DisplayPaymentMethod(cardType: String = "visa", cardExp: String, cardNumber: String)

  def fromOrder(order: Order)(implicit ec: ExecutionContext, db: Database): DBIO[Root] = {
    fetchOrderDetails(order).map { case (customer, items, shipMethod, shipAddress, payment, assignees) ⇒
      build(
        order = order,
        customer = customer,
        lineItems = items,
        shippingAddress = shipAddress,
        shippingMethod = shipMethod,
        assignments = assignees,
        payment = payment
      )
    }
  }

  def build(order: Order, lineItems: Seq[(Sku, OrderLineItem)] = Seq.empty, adjustments: Seq[Adjustment] = Seq.empty,
    shippingMethod: Option[ShippingMethod] = None, customer: Option[Customer] = None,
    shippingAddress: Option[OrderShippingAddress] = None, payment: Option[(OrderPayment, CreditCard)] = None,
    assignments: Seq[(OrderAssignment, StoreAdmin)] = Seq.empty): Root = {

    val displayPayment = payment.map { case (op, cc) ⇒
      DisplayPayment(
        amount = op.amount.getOrElse(0),
        status = "fixme",
        paymentMethod = DisplayPaymentMethod(
          cardExp = s"${cc.expMonth}/${cc.expYear}",
          cardNumber = s"xxx-xxxx-xxxx-${cc.lastFour}"
        )
      )
    }

    Root(id = order.id,
      referenceNumber = order.referenceNumber,
      orderStatus = order.status,
      shippingStatus = order.status,
      paymentStatus = order.status,
      lineItems = lineItems.map { case (sku, li) ⇒ DisplayLineItem(sku = sku.sku, status = li.status) },
      adjustments = adjustments,
      fraudScore = scala.util.Random.nextInt(100),
      customer = customer,
      shippingAddress = shippingAddress,
      totals = Totals(subTotal = 333, taxes = 10, adjustments = 0, total = 510),
      shippingMethod = shippingMethod,
      assignees = assignments.map((AssignmentResponse.build _).tupled),
      remorsePeriodEnd = order.getRemorsePeriodEnd,
      payment = displayPayment
    )
  }

  private def fetchOrderDetails(order: Order)(implicit ec: ExecutionContext) = {
    val shippingMethodQ = for {
      shipment ← Shipments.filter(_.orderId === order.id)
      shipMethod ← models.ShippingMethods.filter(_.id === shipment.shippingMethodId)
    } yield shipMethod

    val paymentQ = for {
      payment ← OrderPayments.filter(_.orderId === order.id)
      creditCard ← CreditCards.filter(_.id === payment.paymentMethodId)
    } yield (payment, creditCard)

    for {
      customer ← Customers._findById(order.customerId).extract.one
      lineItems ← (for {
        li  ← OrderLineItems._findByOrderId(order.id)
        sku ← Skus if sku.id === li.skuId
      } yield (sku, li)).result
      shipMethod ← shippingMethodQ.one
      shipAddress ← OrderShippingAddresses.filter(_.orderId === order.id).one
      payments ← paymentQ.one
      assignments ← OrderAssignments.filter(_.orderId === order.id).result
      admins ← StoreAdmins.filter(_.id.inSetBind(assignments.map(_.assigneeId))).result
    } yield (customer, lineItems, shipMethod, shipAddress, payments, assignments.zip(admins))
  }
}
