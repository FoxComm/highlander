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
    orderStatus: Order.Status,
    paymentStatus: Option[String],
    placedAt: Option[Instant],
    remorsePeriodEnd: Option[Instant],
    total: Option[Int]
    ) extends ResponseItem

  def build(order: Order, customer: Customer, payment: Option[OrderPayment])
    (implicit ec: ExecutionContext): DBIO[Root] = {
    OrderTotaler.grandTotal(order).map { grandTotal ⇒
      Root(
        referenceNumber = order.referenceNumber,
        email = customer.email,
        orderStatus = order.status,
        // TODO: FIXME
        paymentStatus = None,
        placedAt = order.placedAt,
        remorsePeriodEnd = order.getRemorsePeriodEnd,
        total = grandTotal
      )
    }
  }
}
