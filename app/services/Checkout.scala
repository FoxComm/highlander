package services

import models._
import models.Order.Status
import org.scalactic.{Good, Bad, ErrorMessage, Or}
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import scala.concurrent.{Future, ExecutionContext}

import slick.driver.PostgresDriver.api._

class Checkout(order: Order)(implicit ec: ExecutionContext, db: Database) {

  def checkout: Future[Order Or List[ErrorMessage]] = {
    // Realistically, what we'd do here is actually
    // 0) Check that line items exist -- DONE
    // 1) Check Inventory
    // 2) Verify Payment (re-auth)
    // 3) Validate addresses
    // 4) Validate promotions/coupons
    // 5) Final Auth on the payment

    OrderLineItems.countByOrder(this.order).flatMap { count =>
      if (count > 0) {
        authenticatePayments.flatMap { payments =>
          val errors = payments.values.toList.flatten
          if (errors.isEmpty) {
            completeOrderAndCreateNew(order).map(Good(_))
          } else {
            Future.successful(Bad(errors))
          }
        }
      } else {
        Future.successful(Bad(List("No Line Items in Order!")))
      }
    }
  }

  // sets incoming order.status == Order.ordered and creates a new order
  def completeOrderAndCreateNew(order: Order): Future[Order] = {
    val newOrder = Order(customerId = order.customerId, status = Order.Cart)

    db.run(for {
      _ <- Orders._findById(order.id).map(_.status).update(Order.Ordered)
      insertId <- Orders.returningId += newOrder
    } yield newOrder.copy(id = insertId))
  }

  def authenticatePayments: Future[Map[AppliedPayment, List[ErrorMessage]]] = {
    AppliedPayments.findAllPaymentsFor(this.order).flatMap { records =>
      val accum = Future.successful(Map[AppliedPayment, List[ErrorMessage]]())
      records.foldLeft(accum) { case (errors, (payment, creditCard)) =>
        val e = errors.value.get.get // TODO: no, no, no
        creditCard.authenticate(payment.appliedAmount).map { result =>
          e.updated(payment,
            result.fold({ good => List.empty }, { bad => bad }))
        }
      }
    }
  }

  def validateAddresses: List[ErrorMessage] = {
    List.empty
  }
}
