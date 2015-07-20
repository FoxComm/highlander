package services

import scala.concurrent.{ExecutionContext, Future}

import models._
import org.scalactic.{Bad, Good, Or}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}

class Checkout(order: Order)(implicit ec: ExecutionContext, db: Database) {

  def checkout: Future[Order Or List[Failure]] = {
    // Realistically, what we'd do here is actually
    // 0) Check that line items exist -- DONE
    // 1) Check Inventory
    // 2) Verify Payment (re-auth)
    // 3) Validate addresses
    // 4) Validate promotions/coupons
    // 5) Final Auth on the payment
    // 6) Check & Reserve inventory

    OrderLineItems.countByOrder(this.order).flatMap { count =>
      if (count > 0) {
        authorizePayments.flatMap { payments =>
          val errors = payments.values.toList.flatten
          if (errors.isEmpty) {
            completeOrderAndCreateNew(order).map(Good(_))
          } else {
            Future.successful(Bad(errors))
          }
        }
      } else {
        Future.successful(Bad(List(NotFoundFailure("No Line Items in Order!"))))
      }
    }
  }

  // sets incoming order.status == Order.ordered and creates a new order
  def completeOrderAndCreateNew(order: Order): Future[Order] = {
    db.run(for {
      _ <- Orders._findById(order.id).extract.map(_.status).update(Order.Ordered)
      newOrder <- Orders._create(Order.buildCart(order.customerId))
    } yield newOrder)
  }

  def decrementInventory(order: Order): Future[Int] =
    InventoryAdjustments.createAdjustmentsForOrder(order)

  def authorizePayments: Future[Map[OrderPayment, List[Failure]]] = {
    OrderPayments.findAllPaymentsFor(this.order).flatMap { records =>
      Future.sequence(records.map { case (payment, creditCard) =>
        creditCard.authorize(payment.appliedAmount).flatMap { or ⇒
          or.fold({ chargeId ⇒
            val paymentWithCharge = payment.copy(chargeId = Some(chargeId))
            OrderPayments.update(paymentWithCharge).map { _ ⇒
              (paymentWithCharge, or)
            }
          }, { _ ⇒
            Future.successful((payment, or))
          })
        }
      }).map { (results: Seq[(OrderPayment, Or[String, List[Failure]])]) =>
        results.foldLeft(Map[OrderPayment, List[Failure]]()) { case (errors, (payment, result)) =>
          errors.updated(payment,
            result.fold({ good => List.empty }, { bad => bad }))
        }
      }
    }
  }

  def validateAddresses: List[Failure] = {
    List.empty
  }
}
