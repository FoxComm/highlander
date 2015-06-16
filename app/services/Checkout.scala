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
    // 0) Check that line items exist -- DONE
    // 1) Check Inventory
    // 2) Verify Payment (re-auth)
    // 3) Validate addresses
    // 4) Validate promotions/coupons
    // 5) Final Auth on the payment

    CartLineItems.countByCart(this.cart).flatMap { count =>
      if (count > 0) {
        authenticatePayments.flatMap { payments =>
          val errors = payments.values.toList.flatten
          if (errors.isEmpty) {
            buildOrderFromCart(cart).map(Good(_))
          } else {
            Future.successful(Bad(errors))
          }
        }
      } else {
        Future.successful(Bad(List("No Line Items in Cart!")))
      }
    }

  }

  // sets incoming cart.status == Cart.ordered and creates a new cart
  def setCartToOrdered(cart: Cart): DBIOAction[Cart, NoStream, Effect.Write with Effect.Write] = {
    val newCart = Cart(accountId = cart.accountId, status = Cart.Active)
    for {
      _ <- Carts._findById(cart.id).map(_.status).update(Cart.Ordered)
      insertId <- Carts.returningId += newCart
    } yield newCart.copy(id = insertId)
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
            Future.successful(p -> List("There are no payment methods on this order!"))
        }
      }

      Future.sequence(seq).map(_.toMap)
    }
  }

  def validateAddresses: List[ErrorMessage] = {
    List.empty
  }

  def buildOrderFromCart(cart: Cart)(implicit ec: ExecutionContext, db: Database): Future[Order] = {
    val order = Order(customerId = cart.accountId.getOrElse(0), status = Order.New, locked = 0)

    val actions = for {
      newOrderId <- Orders.returningId += order
      items <- CartLineItems.table.filter(_.cartId === cart.id).result
      copiedLineItemIds <- OrderLineItems.returningId ++= items.map { i => new OrderLineItem(orderId = newOrderId, skuId = i.skuId, status = OrderLineItem.New) }
      _ <- setCartToOrdered(cart)
    } yield order.copy(id = newOrderId)

    db.run(actions.transactionally)
  }
}
