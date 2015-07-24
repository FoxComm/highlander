package responses

import scala.concurrent.{ExecutionContext, Future}

import models._
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}

object AdminOrders {
  type Response = Future[Seq[Root]]

  final case class Root(
    id: Int,
    referenceNumber: String,
    email: String,
    orderStatus: Order.Status,
    paymentStatus: String
    )

  def findAll(implicit ec: ExecutionContext, db: Database): Response = {
    val queries = for {
      order ← Orders
      email ← Customers._findEmailById(order.customerId)
      payment ← OrderPayments._findAllPaymentsFor(order.id) // Fails if there are no payments
    } yield (order, email, payment)

    db.run(queries.result).map { results ⇒
      results.map { case (order, email, payment) ⇒
        build(order, email, payment)
      }
    }
  }

  def build(order: Order, email: String, payment: (OrderPayment, CreditCard)): Root = {
    Root(
      id = order.id,
      referenceNumber = order.referenceNumber,
      email = email,
      orderStatus = order.status,
      paymentStatus = payment._1.status
    )
  }
}
