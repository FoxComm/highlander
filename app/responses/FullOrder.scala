package responses

import models._

import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import scala.concurrent.{ExecutionContext, Future}

object FullOrder {
  type Response = Future[Option[Root]]

  final case class Totals(subTotal: Int, taxes: Int, adjustments: Int, total: Int)
  final case class Root(id: Int, referenceNumber: String,
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
                  payments: Option[Seq[DisplayPayment]] = None
    )

  final case class DisplayLineItem(imagePath: String = "http://lorempixel.com/75/75/fashion" ,
                              name: String = "donkey product",
                              skuId: Int,
                              price: Int = 33,
                              qty: Int = 1,
                              status: OrderLineItem.Status )

  final case class DisplayPayment(amount: Int, status: String, referenceNumber: String = "ABC123", paymentMethod: DisplayPaymentMethod)
   //
  // TODO:
  // Capture reference number

  final case class DisplayPaymentMethod(cardType: String = "visa", cardExp: String, cardNumber: String)

  def build(order: Order, lineItems: Seq[OrderLineItem] = Seq.empty, adjustments: Seq[Adjustment] = Seq.empty,
    shippingMethod: Option[ShippingMethod] = None, customer: Option[Customer] = None,
    shippingAddress: Option[Address] = None, orderPayments: Seq[OrderPayment] = Seq.empty,
    creditCards: Seq[CreditCard] = Seq.empty
    ): Root = {

    //TODO: This isn't very robust; make it elegantly handle multiple payments
    val dispPayments = orderPayments.flatMap { op ⇒
      creditCards.filter(_.id == op.paymentMethodId).map { cc ⇒
        DisplayPayment(
          amount = op.appliedAmount, status = op.status,
          paymentMethod = DisplayPaymentMethod(cardExp = (cc.expMonth + "/" + cc.expYear), cardNumber = ("xxx-xxxx-xxxx-" + cc.lastFour))
        )

      }
    }

    Root(id = order.id,
      referenceNumber = order.referenceNumber,
      orderStatus = order.status,
      shippingStatus = order.status,
      paymentStatus = order.status,
      lineItems = lineItems.map{oli => DisplayLineItem(skuId = oli.skuId, status = oli.status)},
      adjustments = adjustments,
      fraudScore = scala.util.Random.nextInt(100),
      customer = customer,
      shippingAddress = shippingAddress,
      totals = Totals(subTotal = 333, taxes = 10, adjustments = 0, total = 510), shippingMethod = shippingMethod,
      payments = Some(dispPayments)
    )
  }


  def findById(id: Int)(implicit ec: ExecutionContext, db: Database): Response =
    this.findOrder(Orders._findById(id).extract)

  def findByRefNum(refNum: String)(implicit ec: ExecutionContext, db: Database): Response =
    this.findOrder(Orders.findByRefNum(refNum))

  def findByCustomer(customer: Customer)
                    (implicit ec: ExecutionContext, db: Database): Response = {
    this.findOrder(Orders._findByCustomer(customer))
  }

  def findCartByCustomer(customer: Customer)(implicit ec: ExecutionContext, db: Database): Response = {
    this.findOrder(Orders._findActiveOrderByCustomer(customer))
  }

  def fromOrder(order: Order)(implicit ec: ExecutionContext, db: Database): Response = {
    val queries = for {
      lineItems <- OrderLineItems._findByOrderId(order.id)
    } yield (lineItems)

    db.run(queries.result).map { lineItems => Some(build(order, lineItems)) }
  }

  private [this] def findOrder(finder: Query[Orders, Order, Seq])
    (implicit ec: ExecutionContext, db: Database): Response = {

    type OrderDetailsQuery = DBIOAction[(Option[Customer], Seq[OrderLineItem], Option[(Address, ShippingMethod)], Seq[
      (OrderPayment, CreditCard)]), NoStream, Effect.Read]
    val noOrder: OrderDetailsQuery = DBIO.successful((None, Seq.empty, None, Seq.empty))

    def fetchOrderDetails(order: Order)(implicit ec: ExecutionContext): OrderDetailsQuery = {
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
        customer ← Customers._findById(order.customerId).extract.result.headOption
        lineItems ← OrderLineItems._findByOrderId(order.id).result
        shipment ← shipmentQ.result.headOption
        payments ← paymentQ.result
      } yield (customer, lineItems, shipment, payments)
    }


    val queries = for {
      order ← finder.result.headOption
      orderDetails ← order.fold(noOrder)(order ⇒ fetchOrderDetails(order))
    } yield (order, orderDetails)

    db.run(queries).map { case (order, (c, items, shipment, payments)) ⇒
      order.map { o ⇒
        val paymentsAndCards = payments.unzip
        build(
          order = o,
          customer = c,
          lineItems = items,
          shippingAddress = shipment.map(_._1),
          shippingMethod = shipment.map(_._2),
          orderPayments = paymentsAndCards._1,
          creditCards = paymentsAndCards._2
        )
      }
    }
  }
}
