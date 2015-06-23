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
        authorizePayments.flatMap { payments =>
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

  def decrementInventory(order: Order): Future[Unit] = {
    OrderLineItems.countBySkuIdForOrder(order).map { items =>
      // TODO(yax): change to batch via foldLeft -> Map
      items.map { case (skuId, count) =>
        InventoryAdjustments.save(InventoryAdjustment(skuId = skuId, inventoryEventId = order.id,
          reservedForFulfillment = count))
      }
    }

    // Step 1) Create inventory adjustment event and map it to the order_id. --> +1 to reserved_for_fullfilment
    // Step 2) Adjust the inventory_summary table accordingly.  ExistingNumber-1 to available on hand.
    // Step 3) Do the above as a transaction.
  }

  def authorizePayments: Future[Map[AppliedPayment, List[ErrorMessage]]] = {
    AppliedPayments.findAllPaymentsFor(this.order).flatMap { records =>
      Future.sequence(records.map { case (payment, creditCard) =>
        creditCard.authorize(payment.appliedAmount).flatMap { or ⇒
          or.fold({ chargeId ⇒
            val paymentWithCharge = payment.copy(chargeId = Some(chargeId))
            AppliedPayments.update(paymentWithCharge).map { _ ⇒
              (paymentWithCharge, or)
            }
          }, { _ ⇒
            Future.successful((payment, or))
          })
        }
      }).map { (results: Seq[(AppliedPayment, Or[String, List[ErrorMessage]])]) =>
        results.foldLeft(Map[AppliedPayment, List[ErrorMessage]]()) { case (errors, (payment, result)) =>
          errors.updated(payment,
            result.fold({ good => List.empty }, { bad => bad }))
        }
      }
    }
  }

  def validateAddresses: List[ErrorMessage] = {
    List.empty
  }
}
