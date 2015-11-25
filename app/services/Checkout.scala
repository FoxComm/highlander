package services

import java.time.Instant

import scala.concurrent.ExecutionContext

import cats.implicits._
import models.Order.RemorseHold
import models._
import services.orders.OrderTotaler
import slick.driver.PostgresDriver.api._
import utils.Apis
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Litterbox._
import utils.Slick.DbResult
import utils.Slick.implicits._
import OrderPayments.scope._
import utils.Litterbox._
import utils.{DbResultT, TableQueryWithId, friendlyClassName}

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
final case class Checkout(cart: Order, cartValidator: CartValidation)
  (implicit db: Database, ec: ExecutionContext, apis: Apis) {

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
      gcAdjs        = giftCards.getOrElse(List.empty).foldLeft(0)(_ + _.getAmount.abs)
      scAdjs        = storeCredits.getOrElse(List.empty).foldLeft(0)(_ + _.getAmount.abs)

      total         ← OrderTotaler.grandTotal(cart).map(_.getOrElse(0))
      ccs           ← authCreditCard(orderTotal = total, internalPaymentTotal = gcAdjs + scAdjs)
    } yield (giftCards, storeCredits)).map { case (gc, sc) ⇒
      // not-so-easy-way to combine error messages from both Xors
      gc.map(_ ⇒ {}).combine(sc.map(_ ⇒ {}))
    }
  }

  private def authGiftCards: DbResult[Seq[GiftCardAdjustment]] = {
    val payments = OrderPayments.findAllGiftCardsByOrderId(cart.id).result
    authInternalPaymentMethod(payments)(GiftCards.authOrderPayment)
  }

  private def authStoreCredits: DbResult[Seq[StoreCreditAdjustment]] = {
    val payments = OrderPayments.findAllStoreCreditsByOrderId(cart.id).result
    authInternalPaymentMethod(payments)(StoreCredits.authOrderPayment)
  }

  private def authInternalPaymentMethod[M, Adj]
  (dbio: DBIO[Seq[(OrderPayment, M)]])(auth: (M, OrderPayment) ⇒ DbResult[Adj]): DbResult[Seq[Adj]] = {
    dbio.flatMap { results ⇒
      if (results.isEmpty)
        DbResult.good(List.empty[Adj])
      else {
        val auths = results.map { case (pmt, m) ⇒ DbResultT(auth(m, pmt)) }
        DbResultT.sequence(auths).value
      }
    }
  }

  private def authCreditCard(orderTotal: Int, internalPaymentTotal: Int): DbResult[Option[CreditCardCharge]] = {
    import scala.concurrent.duration._

    val authAmount = orderTotal - internalPaymentTotal

    if (authAmount > 0) {
      (for {
        pmt   ← OrderPayments.findAllCreditCardsForOrder(cart.id)
        card  ← pmt.creditCard
      } yield (pmt, card)).one.flatMap {
        case Some((pmt, card)) ⇒
          val f = Stripe().authorizeAmount(card.gatewayCustomerId, authAmount, cart.currency)

          (for {
            // TODO: remove the blocking Await which causes us to change types (I knew it was coming anyways!)
            stripeCharge  ← * <~ scala.concurrent.Await.result(f, 5.seconds)
            ourCharge     = CreditCardCharge.authFromStripe(card, pmt, stripeCharge, cart.currency)
            created       ← * <~ CreditCardCharges.create(ourCharge)
          } yield created.some).value

        case None ⇒
          DbResult.failure(GeneralFailure("not enough payment"))
      }
    } else {
      DbResult.none
    }
  }

  private def remorseHold: DbResult[Order] = (for {
    remorseHold  ← * <~ Orders.update(cart, cart.copy(status = RemorseHold, placedAt = Instant.now.some))

    onHoldGcs    ← * <~ (for {
      items ← OrderLineItemGiftCards.findByOrderId(cart.id).result
      holds ← GiftCards
        .filter(_.id.inSet(items.map(_.giftCardId)))
        .map(_.status).update(GiftCard.OnHold)
    } yield holds).toXor

  } yield remorseHold).value

  private def createNewCart: DbResult[Order] =
    Orders.create(Order.buildCart(cart.customerId))
}
