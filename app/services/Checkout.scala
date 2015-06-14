package services

import models._
import models.Order.Status
import org.scalactic.{Good, Bad, ErrorMessage, Or}
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import scala.concurrent.{Future, ExecutionContext}

import slick.driver.PostgresDriver.api._

class Checkout(cart: Cart)(implicit ec: ExecutionContext, db: Database) {

  def checkout: Future[Order Or List[ErrorMessage]] = {
    // Realistically, what we'd do here is actually
    // 1) Check Inventory
    // 2) Verify Payment (re-auth)
    // 3) Validate addresses
    // 4) Validate promotions/couponsi
    // 5) Final Auth on the payment

    val newOrder = buildOrderFromCart(cart)


    // We can asynchronously call the clearCart function because we don't necessarily need it to be cleared right away
    // The next time we need an active cart is when the user adds another product to it.
    // TODO: Implement more robust concurrency mechanism
    clearCart(cart)

    buildOrderFromCart(cart).map(Good(_))
    //Good(newOrder)
  }

  def verifyInventory: List[ErrorMessage] = {
    // TODO: Call the inventory service and verify that inventory exists for all items in cart
    List.empty
  }

  def clearCart(cart: Cart): Unit = {
    val oldCartStatus = for {c <- Carts.table if c.id === cart.id} yield c.status
    val actions = for {
      _ <- oldCartStatus.update(Cart.Status.Ordered)
      insertCartId <- Carts.returningId += Cart(id =0, accountId = cart.accountId, status = Cart.Status.Active)
    } yield (insertCartId)

    db.run(actions)
  }

  def authenticatePayments: Future[Map[AppliedPayment, List[ErrorMessage]]] = {
    // Really, this should authenticate all payments, at their specified 'applied amount.'
    cart.payments.flatMap { payments =>
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

  def buildOrderFromCart(cart: Cart)(implicit ec: ExecutionContext, db: Database): Future[Order] = {
    val order = Order(customerId = cart.accountId.getOrElse(0), status = Status.New, locked = 0)

    val actions = for {
      orderId <- Orders.returningId += order
      items <- CartLineItems.table.filter(_.cartId === cart.id).result
      copiedLineItemIds <- CartLineItems.returningId ++= items.map { i => i.copy(cartId = orderId) }
    } yield (order.copy(id = orderId))

    db.run(actions.transactionally)
  }
}
