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
        // Figure out what to really return if an order status is completed.
        completeOrderAndCreateNew(order).map(Good(_))
      } else {
        Future.successful(Bad(List("No Line Items in Cart!")))
      }
    }

  }

  // sets incoming order.status == Cart.ordered and creates a new order
  def completeOrderAndCreateNew(order: Order): Future[Order] = {
    val newOrder = Order(customerId = order.customerId, status = Order.Cart)


    db.run(for {
      _ <- Orders._findById(order.id).map(_.status).update(Order.Ordered)
      insertId <- Orders.returningId += newOrder
    } yield newOrder.copy(id = insertId))
  }

  def authenticatePayments: Future[Map[AppliedPayment, List[ErrorMessage]]] = {
    // Really, this should authenticate all payments, at their specified 'applied amount.'
    order.payments.flatMap { payments =>
      val seq = payments.map { p =>
        PaymentMethods.findById(p.paymentMethodId).flatMap {
          case Some(c) =>
            val paymentAmount = p.appliedAmount
            c.authenticate(paymentAmount).map {
              case Bad(errors) =>
                p -> errors
              case Good(success) =>
                p -> List[ErrorMessage]()
            }
          case None =>
            Future.successful(p -> List[ErrorMessage]())
        }
      }

      Future.sequence(seq).map(_.toMap)
    }
  }

  def validateAddresses: List[ErrorMessage] = {
    List.empty
  }

}
