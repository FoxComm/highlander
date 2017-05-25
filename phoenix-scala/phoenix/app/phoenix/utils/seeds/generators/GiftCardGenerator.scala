package phoenix.utils.seeds.generators

import core.db._
import core.utils.Money.Currency
import objectframework.models.ObjectContext
import phoenix.models.account.Scope
import phoenix.models.cord.{Cart, Carts, Order, Orders}
import phoenix.models.payment.giftcard._
import phoenix.payloads.GiftCardPayloads.GiftCardCreateByCsr
import phoenix.utils.aliases._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

trait GiftCardGenerator {

  def nextGcBalance = {
    val prices = Seq(1000, 2500, 3000, 5000, 7500, 10000, 15000, 20000)
    prices(Random.nextInt(prices.length))
  }

  def generateGiftCardAppeasement(implicit db: DB, au: AU): DbResultT[GiftCard] =
    for {
      origin ← * <~ GiftCardManuals.create(GiftCardManual(adminId = 1, reasonId = 1))
      scope  ← * <~ Scope.resolveOverride()
      gc ← * <~ GiftCards.create(
              GiftCard.buildAppeasement(GiftCardCreateByCsr(balance = nextGcBalance, reasonId = 1),
                                        originId = origin.id,
                                        scope = scope))
    } yield gc

  def generateGiftCardPurchase(accountId: Int, context: ObjectContext)(
      implicit db: DB,
      au: AU): DbResultT[GiftCard] =
    for {
      cart  ← * <~ Carts.create(Cart(accountId = accountId, scope = Scope.current))
      order ← * <~ Orders.createFromCart(cart, context.id, None)
      order ← * <~ Orders.update(order, order.copy(state = Order.ManualHold))
      orig  ← * <~ GiftCardOrders.create(GiftCardOrder(cordRef = order.refNum))
      gc ← * <~ GiftCards.create(
              GiftCard.build(balance = nextGcBalance, originId = orig.id, currency = Currency.USD))
    } yield gc
}
