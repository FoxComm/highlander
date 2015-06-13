package services

import models._
import models.Order.Status
import org.scalactic.{Good, Bad, ErrorMessage, Or}
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import scala.concurrent.{Future, ExecutionContext}

import slick.driver.PostgresDriver.api._

class Checkout(cart: Cart)(implicit ec: ExecutionContext, db: Database) {

  def checkout: Order Or List[ErrorMessage] = {
    // Realistically, what we'd do here is actually
    // 1) Check Inventory
    // 2) Verify Payment (re-auth)
    // 3) Validate addresses
    // 4) Validate promotions/couponsi
    // 5) Final Auth on the payment
    val order = Order(id = 0, customerId = cart.accountId.getOrElse(0), status = Order.Status.New, locked = 0)

    //    if (scala.util.Random.nextInt(2) == 1) {
    //      Bad(List("payment re-auth failed"))
    //    } else {
    //      Good(order)
    //    }
    Good(order)
  }

  def verifyInventory: List[ErrorMessage] = {
    // TODO: Call the inventory service and verify that inventory exists for all items in cart
    List.empty
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

  // GAH!  I tried to do this transactionally, and failed miserably.
  def buildOrderFromCart(cart: Cart)(implicit ec: ExecutionContext, db: Database): Future[Order] = {
    val lineTable = TableQuery[LineItems]
    val returningLineItem = lineTable.returning(lineTable.map(_.id))
    val insertOrder = Orders._create(order = new Order(id = 0, customerId = cart.accountId.getOrElse(0), status = Status.New, locked = 0))
    db.run(insertOrder)
//      val lineQuery = lineTable.filter(_.parentId === cart.id).filter(_.parentType === "cart").result.map { lineItems =>
//        lineItems.foreach { lineItem =>
//          returningLineItem += lineItem.copy(parentId = insertedOrder.id, parentType = "order")
//        }
//      }

      //    db.run(lineQuery).map { donkey =>
      //      Seq(1, 2, 3)
      //    }
    }


}
