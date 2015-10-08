package services

import java.time.Instant

import scala.concurrent.{ExecutionContext, Future}

import cats.data.Xor
import models._
import collection.immutable

import slick.driver.PostgresDriver.api._



class Checkout(order: Order)(implicit ec: ExecutionContext, db: Database) {

  def checkout: Result[Order] = {
    // Realistically, what we'd do here is actually
    // 0) Check that line items exist -- DONE
    // 1) Check Inventory
    // 2) Verify Payment (re-auth)
    // 3) Validate addresses
    // 4) Validate promotions/coupons
    // 5) Final Auth on the payment
    // 6) Check & Reserve inventory

    hasLineItems.flatMap { has =>
      if (has) {
        authorizePayments.flatMap { payments =>
          val errors = payments.values.toList.flatten
          if (errors.isEmpty) {
//            completeOrderAndCreateNew(order).map(Good(_))
            Result.good(order)
          } else {
            Result.failures(errors: _*)
          }
        }
      } else {
        Result.failure(NotFoundFailure("No Line Items in Order!"))
      }
    }
  }

  def authorizePayments: Future[Map[OrderPayment, List[Failure]]] = {
    for {
      payments ← OrderPayments.findAllPaymentsFor(order)
      authorized ← Future.sequence(authorizePayments(payments))
    } yield updatePaymentsWithAuthorizationErrors(authorized)
  }

  def decrementInventory(order: Order): Future[Int] =
    InventoryAdjustments.createAdjustmentsForOrder(order)

  def validateAddresses: Failures = {
    Failures()
  }

  private def authorizePayments(payments: Seq[(OrderPayment, CreditCard)]) = {
    for {
      (payment, creditCard) <- payments
    } yield authorizePayment(payment, creditCard)
  }

  // TODO: we must do this *after* auth'ing GC/SC
  private def authorizePayment(payment : OrderPayment, creditCard : CreditCard) = {
    creditCard.authorize(payment.amount.getOrElse(-50)).flatMap { xor ⇒
      xor.fold(
      { _ ⇒ Future.successful((payment, xor))},
      { chargeId ⇒  updateOrderPaymentWithCharge(payment, chargeId, xor) })
    }
  }

  private def updatePaymentsWithAuthorizationErrors(payments: Seq[(OrderPayment, Xor[Failures, String])]) = {
    payments.foldLeft(Map.empty[OrderPayment, List[Failure]]) {
      case (pmts, (payment, result)) =>
        updatePaymentWithAuthorizationErrors(pmts, payment, result)
    }
  }

  private def updatePaymentWithAuthorizationErrors(
    payments: Map[OrderPayment, List[Failure]],
    payment:  OrderPayment,
    result:  Failures Xor String) =
      payments.updated(
        payment,
        result.fold(bad ⇒ bad.toList, good ⇒ Nil))

  // sets incoming order.status == Order.ordered and creates a new order
  private def completeOrderAndCreateNew(order: Order): Future[Order] = {
    db.run(for {
      _ ← Orders._findById(order.id).extract
        .map { o => (o.status, o.placedAt) }
        .update((Order.Ordered, Some(Instant.now)))

        newOrder <- Orders._create(Order.buildCart(order.customerId))
    } yield newOrder)
  }

  private def updateOrderPaymentWithCharge(payment : OrderPayment, chargeId : String, or: Failures Xor String) = {
    OrderPayments.update(payment).map { _ ⇒ (payment, or) }
  }

  private def hasLineItems = {
    for { count <- OrderLineItems.countByOrder(order) } yield (count > 0)
  }



}
