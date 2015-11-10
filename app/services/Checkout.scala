package services

import java.time.Instant

import scala.concurrent.{ExecutionContext, Future}

import cats.data.Xor
import cats.data.Validated.{valid, invalid}
import cats.implicits._
import models.Order.RemorseHold
import models._
import collection.immutable
import payloads.StoreCreditPayment
import slick.driver.PostgresDriver.api._
import utils.Slick.DbResult
import utils.Slick.UpdateReturning._
import utils.Slick.implicits._
import OrderPayments.scope._
import utils.Litterbox._
import utils.friendlyClassName

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
        } yield order).transactionally.run()
    }
  }

  private def checkInventory: DbResult[Unit] = DbResult.unit

  private def activePromos: DbResult[Unit] = DbResult.unit

  private def authPayments: DbResult[Unit] = {
    (for {
      giftCards     ← authGiftCards
      storeCredits  ← authStoreCredits
    } yield (giftCards, storeCredits)).map { case (gc, sc) ⇒
      // not-so-easy-way to combine error messages from both Xors
      gc.map(_ ⇒ {}).combine(sc.map(_ ⇒ {}))
    }
  }

  private def authGiftCards: DbResult[List[GiftCardAdjustment]] = {
    (for {
      pmts ← OrderPayments.filter(_.orderId === cart.id)
      gc ← GiftCards if gc.id === pmts.paymentMethodId
    } yield (pmts, gc)).result.flatMap { results ⇒
      if (results.isEmpty)
        DbResult.good(List.empty[GiftCardAdjustment])
      else {
        val auths = results.map { case (pmt, gc) ⇒
         val auth = GiftCards.auth(giftCard = gc, orderPaymentId = pmt.id.some, debit = pmt.amount.getOrElse(0))
          DbResult.fromDbio(auth)
        }

        DBIO.sequence(auths).map(_.toList.sequenceU)
      }
    }
  }

  private def authStoreCredits: DbResult[List[StoreCreditAdjustment]] = {
    (for {
      pmts ← OrderPayments.filter(_.orderId === cart.id)
      sc ← StoreCredits if sc.id === pmts.paymentMethodId
    } yield (pmts, sc)).result.flatMap { results ⇒
      if (results.isEmpty)
        DbResult.good(List.empty[StoreCreditAdjustment])
      else {
        val auths = results.map { case (pmt, sc) ⇒
          val auth = StoreCredits.auth(storeCredit = sc, orderPaymentId = pmt.id.some, amount = pmt.amount.getOrElse(0))
          DbResult.fromDbio(auth)
        }

        DBIO.sequence(auths).map(_.toList.sequenceU)
      }
    }
  }

  private def remorseHold: DbResult[Order] = {
    val changed = cart.updateTo(cart.copy(status = RemorseHold, placedAt = Instant.now.some))

    changed.fold(DbResult.failures, { c ⇒
      val updateOrder = Orders.findById(cart.id).extract
        .map { o ⇒ (o.status, o.placedAt) }
        .update((c.status, c.placedAt))

      val updateGcs = for {
        items  ← OrderLineItemGiftCards.findByOrderId(cart.id).result
        holds  ← GiftCards
          .filter(_.id.inSet(items.map(_.giftCardId)))
          .map(_.status).update(GiftCard.OnHold)
      } yield holds

      (updateGcs >> updateOrder).flatMap(_ ⇒ DbResult.good(c))
    })
  }

  private def createNewCart: DbResult[Order] =
    Orders.saveNew(Order.buildCart(cart.customerId)).flatMap(DbResult.good)
}
