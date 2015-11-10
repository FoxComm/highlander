package services

import java.time.Instant

import scala.concurrent.{ExecutionContext, Future}

import cats.data.{ValidatedNel, Xor}
import cats.data.Validated.{valid, invalid}
import cats.implicits._
import models.Order.RemorseHold
import models._
import collection.immutable
import slick.driver.PostgresDriver.api._
import utils.Slick.DbResult
import utils.Slick.UpdateReturning._
import utils.Slick.implicits._
import OrderPayments.scope._

/*
  1) Run cart through validator
  2) Check inventory availability for every item (currently, that we have some items since our inventory is bunk)
  3) Re-validate that applied promos are active
  4) Authorize each payment method (stripe for cc, and gc and sc internally)
  5) Transition order to Remorse Hold**
  6) Create new cart for customer
 */
final case class Checkout(cart: Order, cartValidator: CartValidation)(implicit db: Database, ec: ExecutionContext) {
  def checkout: Result[Order] = {
    cartValidator.validate.flatMap {
      case Xor.Left(f) ⇒
        Result.failures(f)

      case Xor.Right(resp) if resp.warnings.nonEmpty ⇒
        Result.failures(resp.warnings: _*)

      case Xor.Right(_) ⇒
        (for {
          _ ← checkInventory
          _ ← activePromos
          _ ← authPayments
          _ ← remorseHold
          order ← createNewCart
        } yield order).run()
    }
  }

  private def checkInventory: DbResult[Unit] = DbResult.unit

  private def activePromos: DbResult[Unit] = DbResult.unit

  private def authPayments: DbResult[Unit] = DbResult.unit

  private def remorseHold: DbResult[Order] = {
    val changed = cart.updateTo(cart.copy(status = RemorseHold, placedAt = Instant.now.some))
    changed.fold(DbResult.failures, { c ⇒
      Orders.findById(cart.id).extract
        .map { o ⇒ (o.status, o.placedAt) }
        .update((c.status, c.placedAt))
        .flatMap(_ ⇒ DbResult.good(c))
    })
  }

  private def createNewCart: DbResult[Order] =
    Orders.saveNew(Order.buildCart(cart.customerId)).flatMap(DbResult.good)
}
