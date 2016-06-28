package utils.seeds.generators

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

import models.objects.ObjectContext
import models.order.{Order, Orders}
import models.payment.giftcard._
import payloads.GiftCardPayloads.GiftCardCreateByCsr
import slick.driver.PostgresDriver.api._
import utils.Money.Currency
import utils.db.DbResultT._
import utils.db._

trait GiftCardGenerator {

  def nextGcBalance = {
    val prices = Seq(1000, 2500, 3000, 5000, 7500, 10000, 15000, 20000)
    prices(Random.nextInt(prices.length))
  }

  def generateGiftCardAppeasement(implicit db: Database): DbResultT[GiftCard] =
    for {
      origin ← * <~ GiftCardManuals.create(GiftCardManual(adminId = 1, reasonId = 1))
      gc ← * <~ GiftCards.create(
              GiftCard.buildAppeasement(GiftCardCreateByCsr(balance = nextGcBalance, reasonId = 1),
                                        originId = origin.id))
    } yield gc

  def generateGiftCardPurchase(customerId: Int, context: ObjectContext)(
      implicit db: Database): DbResultT[GiftCard] =
    for {
      order ← * <~ Orders.create(
                 Order(state = Order.ManualHold, customerId = customerId, contextId = context.id))
      orig ← * <~ GiftCardOrders.create(GiftCardOrder(orderRef = order.refNum))
      gc ← * <~ GiftCards.create(
              GiftCard.build(balance = nextGcBalance, originId = orig.id, currency = Currency.USD))
    } yield gc
}
