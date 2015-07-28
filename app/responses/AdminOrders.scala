package responses

import scala.concurrent.{ExecutionContext, Future}

import models._
import org.joda.time.DateTime
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}

object AdminOrders {
  type Response = Future[Seq[Root]]

  final case class Root(
    id: Int,
    referenceNumber: String,
    email: Option[String],
    orderStatus: Order.Status,
    paymentStatus: Option[String],
    placedAt: Option[DateTime],
    total: Int
    )

  def findAll(implicit ec: ExecutionContext, db: Database): Response = {

    val orderQ = Orders joinLeft Customers on (_.customerId === _.id)

    val paymentQ = for {
      ((order, customer), payment) ← orderQ joinLeft OrderPayments on (_._1.id === _.orderId)
      (_, _) ← OrderPayments joinLeft CreditCards on (_.id === _.id)
    } yield (order, customer, payment)

    db.run(paymentQ.result).map {
      _.map { case (order, customer, payment) ⇒
        build(order, customer, payment)
      }
    }.flatMap(Future.sequence(_))
  }

  def build(order: Order, customer: Option[Customer], payment: Option[OrderPayment])
    (implicit ec: ExecutionContext): Future[Root] = {
    order.grandTotal.map { grandTotal ⇒
      Root(
        id = order.id,
        referenceNumber = order.referenceNumber,
        email = customer.map(_.email),
        orderStatus = order.status,
        paymentStatus = payment.map(_.status),
        placedAt = order.placedAt,
        total = grandTotal
      )
    }
  }
}
