package services

import java.time.Instant

import scala.concurrent.ExecutionContext

import cats.implicits._
import models.Order.RemorseHold
import models._
import responses.TheResponse
import slick.driver.PostgresDriver.api._
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Litterbox._
import utils.Slick.DbResult
import utils.Slick.implicits._
import OrderPayments.scope._
import utils.Litterbox._
import utils.{TableQueryWithId, friendlyClassName}

import utils.DbResultT._
import utils.DbResultT.implicits._

/*
  1) Run cart through validator
  2) Check inventory availability for every item (currently, that we have some items since our inventory is bunk)
  3) Re-validate that applied promos are active
  4) Authorize each payment method (stripe for cc, and gc and sc internally)
  5) Transition order to Remorse Hold**
  6) Create new cart for customer
 */
final case class Checkout(cart: Order, cartValidator: CartValidation)(implicit db: Database, ec: ExecutionContext) {
  def checkout: Result[Order] = (for {
      _     ← * <~ cart.mustBeCart
      _     ← * <~ checkInventory
      _     ← * <~ activePromos
      _     ← * <~ authPayments
      _     ← * <~ remorseHold
      order ← * <~ createNewCart
      valid ← * <~ cartValidator.validate
      resp  ← * <~ valid.warnings.fold(DbResult.good(order))(DbResult.failures)
    } yield resp).runT()

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
    val payments = OrderPayments.findAllGiftCardsByOrderId(cart.id).result
    authInternalPaymentMethod(payments)(GiftCards.authOrderPayment)
  }

  private def authStoreCredits: DbResult[List[StoreCreditAdjustment]] = {
    val payments = OrderPayments.findAllStoreCreditsByOrderId(cart.id).result
    authInternalPaymentMethod(payments)(StoreCredits.authOrderPayment)
  }

  private def authInternalPaymentMethod[M, Adj]
  (dbio: DBIO[Seq[(OrderPayment, M)]])(auth: (M, OrderPayment) ⇒ DBIO[Adj]): DbResult[List[Adj]] = {
    dbio.flatMap { results ⇒
      if (results.isEmpty)
        DbResult.good(List.empty[Adj])
      else {
        val auths = results.map { case (pmt, m) ⇒ auth(m, pmt).toXor }
        DBIO.sequence(auths).map(_.toList.sequenceU)
      }
    }
  }

  private def remorseHold: DbResult[Order] = (for {
    remorseHold ← * <~ cart.updateTo(cart.copy(status = RemorseHold, placedAt = Instant.now.some))

    newCart ← * <~ Orders.update(cart, remorseHold)

    onHoldGcs ← * <~ (for {
      items ← OrderLineItemGiftCards.findByOrderId(cart.id).result
      holds ← GiftCards
        .filter(_.id.inSet(items.map(_.giftCardId)))
        .map(_.status).update(GiftCard.OnHold)
    } yield holds).toXor

  } yield newCart).value

  private def createNewCart: DbResult[Order] =
    Orders.create(Order.buildCart(cart.customerId))
}
