package responses

import scala.concurrent.{ExecutionContext, Future}

import models._
import org.joda.time.DateTime
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}

object AllOrders {
  type Response = Future[Seq[Root]]

  final case class Root(
    referenceNumber: String,
    email: String,
    orderStatus: Order.Status,
    paymentStatus: Option[String],
    placedAt: Option[DateTime],
    total: Int
    )

  def findAll(implicit ec: ExecutionContext, db: Database): Response = {

    val ordersAndCustomers = for {
      (order, customer) ← Orders.join(Customers).on(_.customerId === _.id)
    } yield (order, customer)

    val creditCardPayments = for {
      (orderPayment, creditCard) ← OrderPayments.join(CreditCards).on(_.id === _.id)
    } yield (orderPayment, creditCard)

    val query = ordersAndCustomers.joinLeft(creditCardPayments).on(_._1.id === _._1.orderId)

    db.run(query.result).map {
      _.map { case ((order, customer), payment) ⇒
        build(order, customer, payment.map(_._1))
      }
    }.flatMap(Future.sequence(_))
  }

  def build(order: Order, customer: Customer, payment: Option[OrderPayment])
    (implicit ec: ExecutionContext): Future[Root] = {
    order.grandTotal.map { grandTotal ⇒
      Root(
        referenceNumber = order.referenceNumber,
        email = customer.email,
        orderStatus = order.status,
        paymentStatus = payment.map(_.status),
        placedAt = order.placedAt,
        total = grandTotal
      )
    }
  }
}
