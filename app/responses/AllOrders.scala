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
    )

  def runFindAll(implicit ec: ExecutionContext, db: Database): Response = {
    db.run(findAll)
  }

  def findAll(implicit ec: ExecutionContext, db: Database): DBIO[Seq[Root]] = {
    val ordersAndCustomers = for {
      (order, customer) ← Orders.join(Customers).on(_.customerId === _.id)
    } yield (order, customer)

    val creditCardPayments = for {
      (orderPayment, creditCard) ← OrderPayments.join(CreditCards).on(_.id === _.id)
    } yield (orderPayment, creditCard)

    val query = ordersAndCustomers.joinLeft(creditCardPayments).on(_._1.id === _._1.orderId)

    query.result.flatMap { results ⇒
      DBIO.sequence {
        results.map { case ((order, customer), payment) ⇒
          DBIO.from(build(order, customer, payment.map(_._1)))
        }
      }
    }
  }

  def build(order: Order, customer: Customer, payment: Option[OrderPayment])
    (implicit ec: ExecutionContext): Future[Root] = {
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
