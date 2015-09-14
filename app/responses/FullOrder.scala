package responses

import scala.concurrent.{ExecutionContext, Future}

import models._
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._

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
    shippingAddress: Option[Address],
    payment: Option[DisplayPayment] = None
    )

  final case class DisplayLineItem(
    imagePath: String = "http://lorempixel.com/75/75/fashion",
    name: String = "donkey product",
    skuId: Int,
    price: Int = 33,
    qty: Int = 1,
    status: OrderLineItem.Status
    )

  final case class DisplayPayment(amount: Int, status: String, referenceNumber: String = "ABC123", paymentMethod: DisplayPaymentMethod)
   //
  // TODO:
  // Capture reference number

  final case class DisplayPaymentMethod(cardType: String = "visa", cardExp: String, cardNumber: String)

  def fromOrder(order: Order)(implicit ec: ExecutionContext, db: Database): Response = {

    db.run(fetchOrderDetails(order)).map { case (customer, items, shipment, payment) ⇒
      build(
        order = order,
        customer = customer,
        lineItems = items,
        shippingAddress = shipment.map { case (address, _) ⇒ address },
        shippingMethod = shipment.map { case (_, method) ⇒ method },
        payment = payment
      )
    }
  }

  def build(order: Order, lineItems: Seq[OrderLineItem] = Seq.empty, adjustments: Seq[Adjustment] = Seq.empty,
    shippingMethod: Option[ShippingMethod] = None, customer: Option[Customer] = None,
    shippingAddress: Option[Address] = None, payment: Option[(OrderPayment, CreditCard)] = None): Root = {

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
      lineItems = lineItems.map { oli ⇒ DisplayLineItem(skuId = oli.skuId, status = oli.status) },
      adjustments = adjustments,
      fraudScore = scala.util.Random.nextInt(100),
      customer = customer,
      shippingAddress = shippingAddress,
      totals = Totals(subTotal = 333, taxes = 10, adjustments = 0, total = 510),
      shippingMethod = shippingMethod,
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
      lineItems ← OrderLineItems._findByOrderId(order.id).result
      shipment ← shipmentQ.one
      payments ← paymentQ.one
    } yield (customer, lineItems, shipment, payments)
  }
}
