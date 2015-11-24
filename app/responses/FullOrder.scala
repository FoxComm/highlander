package responses

import java.time.Instant

import scala.concurrent.{ExecutionContext, Future}

import models._
import services._
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._

object FullOrder {
  type Response = Future[Root]
  type Payment = (OrderPayment, CreditCard, CreditCardCharge)

  final case class Totals(subTotal: Int, taxes: Int, adjustments: Int, total: Int) extends ResponseItem

  final case class LineItems(
    skus: Seq[DisplayLineItem] = Seq.empty,
    giftCards: Seq[GiftCardResponse.Root] = Seq.empty
    ) extends ResponseItem

  final case class Root(
    id: Int,
    referenceNumber: String,
    orderStatus: Order.Status,
    shippingStatus: Order.Status,
    paymentStatus: Option[CreditCardCharge.Status],
    lineItems: LineItems,
    adjustments: Seq[Adjustment],
    fraudScore: Int,
    totals: Totals,
    customer: Option[Customer],
    shippingMethod: Option[ShippingMethods.Root],
    shippingAddress: Option[Addresses.Root],
    assignees: Seq[AssignmentResponse.Root],
    remorsePeriodEnd: Option[Instant],
    payment: Option[DisplayPayment] = None) extends ResponseItem

  final case class DisplayLineItem(
    imagePath: String = "http://lorempixel.com/75/75/fashion",
    name: String = "donkey product",
    sku: String,
    price: Int = 33,
    quantity: Int = 1,
    totalPrice: Int = 33,
    status: OrderLineItem.Status) extends ResponseItem

  final case class DisplayPayment(
    amount: Int,
    status: CreditCardCharge.Status,
    referenceNumber: String = "ABC123",
    paymentMethod: DisplayPaymentMethod) extends ResponseItem
   //
  // TODO:
  // Capture reference number

  final case class DisplayPaymentMethod(
    cardType: String = "visa",
    cardExp: String,
    cardNumber: String) extends ResponseItem

  def refreshAndFullOrder(order: Order)(implicit ec: ExecutionContext, db: Database): DBIO[FullOrder.Root] =
    Orders.refresh(order).flatMap(fromOrder)

  def fromOrder(order: Order)(implicit ec: ExecutionContext, db: Database): DBIO[Root] = {
    fetchOrderDetails(order).map { case (customer, skus, shipMethod, shipAddress, payment, assignees, giftCards) ⇒
      build(
        order = order,
        customer = customer,
        skus = skus,
        giftCards = giftCards,
        shippingAddress = shipAddress.toOption,
        shippingMethod = shipMethod,
        assignments = assignees,
        maybePayment = payment
      )
    }
  }

  def build(order: Order, skus: Seq[(Sku, OrderLineItem)] = Seq.empty, adjustments: Seq[Adjustment] = Seq.empty,
    shippingMethod: Option[ShippingMethod] = None, customer: Option[Customer] = None,
    shippingAddress: Option[Addresses.Root] = None, maybePayment: Option[Payment] = None,
    assignments: Seq[(OrderAssignment, StoreAdmin)] = Seq.empty,
    giftCards: Seq[(GiftCard, OrderLineItemGiftCard)] = Seq.empty): Root = {

    val displayPayment = maybePayment.map { case (op, cc, ccc) ⇒
      DisplayPayment(
        amount = op.amount.getOrElse(0),
        status = ccc.status,
        paymentMethod = DisplayPaymentMethod(
          cardExp = s"${cc.expMonth}/${cc.expYear}",
          cardNumber = s"xxx-xxxx-xxxx-${cc.lastFour}"
        )
      )
    }

    val skuList = skus.map { case (sku, li) ⇒ DisplayLineItem(sku = sku.sku, status = li.status) }
    val gcList = giftCards.map { case (gc, li) ⇒ GiftCardResponse.build(gc) }

    Root(id = order.id,
      referenceNumber = order.referenceNumber,
      orderStatus = order.status,
      shippingStatus = order.status,
      paymentStatus = maybePayment.map(p ⇒ getPaymentStatus(order.status, p)),
      lineItems = LineItems(skus = skuList, giftCards = gcList),
      adjustments = adjustments,
      fraudScore = scala.util.Random.nextInt(100),
      customer = customer,
      shippingAddress = shippingAddress,
      totals = Totals(subTotal = 333, taxes = 10, adjustments = 0, total = 510),
      shippingMethod = shippingMethod.map(ShippingMethods.build(_)),
      assignees = assignments.map((AssignmentResponse.build _).tupled),
      remorsePeriodEnd = order.getRemorsePeriodEnd,
      payment = displayPayment
    )
  }

  private def getPaymentStatus(orderStatus: Order.Status, payment: Payment): CreditCardCharge.Status = {
    (orderStatus, payment) match {
      case (Order.Cart, _) ⇒ CreditCardCharge.Cart
      case (_, (_, _, creditCardCharge)) ⇒ creditCardCharge.status
    }
  }

  private def fetchOrderDetails(order: Order)(implicit ec: ExecutionContext) = {
    val shippingMethodQ = for {
      orderShippingMethod ← models.OrderShippingMethods.filter(_.orderId === order.id)
      shipMethod ← models.ShippingMethods.filter(_.id === orderShippingMethod.shippingMethodId)
    } yield shipMethod

    val paymentQ = for {
      payment ← OrderPayments.filter(_.orderId === order.id)
      creditCard ← CreditCards.filter(_.id === payment.paymentMethodId)
      creditCardCharge ← CreditCardCharges.filter(_.orderPaymentId === payment.id)
    } yield (payment, creditCard, creditCardCharge)

    for {
      customer ← Customers.findById(order.customerId).extract.one
      lineItems ← OrderLineItemSkus.findLineItemsByOrder(order).result
      giftCards ← OrderLineItemGiftCards.findLineItemsByOrder(order).result
      shipMethod ← shippingMethodQ.one
      shipAddress ← Addresses.forOrderId(order.id)
      payments ← paymentQ.one
      assignments ← OrderAssignments.filter(_.orderId === order.id).result
      admins ← StoreAdmins.filter(_.id.inSetBind(assignments.map(_.assigneeId))).result
    } yield (customer, lineItems, shipMethod, shipAddress, payments, assignments.zip(admins), giftCards)
  }
}
