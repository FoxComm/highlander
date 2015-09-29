package responses

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
    giftCards: Seq[GiftCardResponse.Root] = Seq.empty,
    adjustments: Seq[Adjustment],
    fraudScore: Int,
    totals: Totals,
    customer: Option[Customer],
    shippingMethod: Option[ShippingMethod],
    shippingAddress: Option[Address],
    assignees: Seq[AssignmentResponse.Root],
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
    fetchOrderDetails(order).map { case (customer, items, shipment, payment, assignees, giftCards) ⇒
      build(
        order = order,
        customer = customer,
        lineItems = items,
        giftCards = giftCards,
        shippingAddress = shipment.map { case (address, _) ⇒ address },
        shippingMethod = shipment.map { case (_, method) ⇒ method },
        assignments = assignees,
        payment = payment
      )
    }
  }

  def build(order: Order, lineItems: Seq[(Sku, OrderLineItem)] = Seq.empty, adjustments: Seq[Adjustment] = Seq.empty,
    shippingMethod: Option[ShippingMethod] = None, customer: Option[Customer] = None,
    shippingAddress: Option[Address] = None, payment: Option[(OrderPayment, CreditCard)] = None,
    assignments: Seq[(OrderAssignment, StoreAdmin)] = Seq.empty,
    giftCards: Seq[(GiftCard, OrderGiftCard)] = Seq.empty): Root = {

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
      giftCards = giftCards.map { case (gc, rel) ⇒ GiftCardResponse.build(gc) },
      adjustments = adjustments,
      fraudScore = scala.util.Random.nextInt(100),
      customer = customer,
      shippingAddress = shippingAddress,
      totals = Totals(subTotal = 333, taxes = 10, adjustments = 0, total = 510),
      shippingMethod = shippingMethod,
      assignees = assignments.map((AssignmentResponse.build _).tupled),
      payment = displayPayment
    )
  }

  private def fetchOrderDetails(order: Order)(implicit ec: ExecutionContext) = {
    val shipmentQ = for {
      shipment ← Shipments.filter(_.orderId === order.id)
      address ← models.Addresses.filter(_.id === shipment.shippingAddressId)
      method ← ShippingMethods.filter(_.id === shipment.shippingMethodId)
    } yield (address, method)

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
      giftCards ← (for {
        relation ← OrderGiftCards.filter(_.orderId === order.id)
        giftCard ← GiftCards if giftCard.id === relation.giftCardId
      } yield (giftCard, relation)).result
      shipment ← shipmentQ.one
      payments ← paymentQ.one
      assignments ← OrderAssignments.filter(_.orderId === order.id).result
      admins ← StoreAdmins.filter(_.id.inSetBind(assignments.map(_.assigneeId))).result
    } yield (customer, lineItems, shipment, payments, assignments.zip(admins), giftCards)
  }
}
