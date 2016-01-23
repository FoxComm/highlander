package responses

import java.time.Instant

import models.{Customer, Order, OrderPayment}
import services.orders.OrderTotaler
import slick.driver.PostgresDriver.api._

import scala.concurrent.{ExecutionContext, Future}

object AllOrders {
  type Response = Future[Seq[Root]]

  final case class Root(
    referenceNumber: String,
    email: String,
    name: Option[String] = None,
    orderState: Order.State,
    paymentState: Option[String] = None,
    shippingState: Option[String] = None,
    placedAt: Option[Instant] = None,
    remorsePeriodEnd: Option[Instant] = None,
    total: Int) extends ResponseItem

  def build(order: Order, customer: Customer, payment: Option[OrderPayment])
    (implicit ec: ExecutionContext): Root = Root(
      referenceNumber = order.referenceNumber,
      name = customer.name,
      email = customer.email,
      orderState = order.state,
      // TODO: FIXME
      paymentState = Some("FIXME"),
      shippingState = Some("FIXME"),
      placedAt = order.placedAt,
      remorsePeriodEnd = order.getRemorsePeriodEnd,
      total = order.grandTotal
  )
}
