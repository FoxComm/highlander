package responses

import java.time.Instant

import scala.concurrent.{ExecutionContext, Future}

import models._
import services.{NotFoundFailure, OrderUpdateFailure}
import slick.driver.PostgresDriver.api._

final case class BulkOrderUpdateResponse(orders: Seq[AllOrders.Root], failures: Seq[OrderUpdateFailure])

final case class BulkAssignmentResponse(orders: Seq[AllOrders.Root], adminNotFound: Option[NotFoundFailure],
  ordersNotFound: Seq[NotFoundFailure])

object AllOrders {
  type Response = Future[Seq[Root]]

  final case class Root(
    referenceNumber: String,
    email: String,
    orderStatus: Order.Status,
    paymentStatus: Option[String],
    placedAt: Option[Instant],
    remorsePeriodEnd: Option[Instant],
    total: Int
    ) extends ResponseItem

  def build(order: Order, customer: Customer, payment: Option[OrderPayment])
    (implicit ec: ExecutionContext): DBIO[Root] = {
    order.grandTotal.map { grandTotal ⇒
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
