package utils.seeds.generators

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random
import models.order.{Orders, Order}
import models.product.{ProductContext}
import models.payment.giftcard.{GiftCard, GiftCards, GiftCardOrders, GiftCardOrder,
  GiftCardManuals, GiftCardManual}
import payloads.GiftCardCreateByCsr
import utils.DbResultT
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Money.Currency
import GeneratorUtils.randomString

import slick.driver.PostgresDriver.api._
import faker._;

trait GiftCardGenerator {

 def nextGcBalance = {
    val prices = Seq(1000, 2500, 3000, 5000, 7500, 10000, 15000, 20000)
    prices(Random.nextInt(prices.length))
  }

  def generateGiftCardAppeasement(implicit db: Database) : DbResultT[GiftCard] = for {
    origin ← * <~ GiftCardManuals.create(GiftCardManual(adminId = 1, reasonId = 1))
    gc    ← * <~ GiftCards.create(GiftCard.buildAppeasement(GiftCardCreateByCsr(balance = nextGcBalance, reasonId = 1), originId = origin.id))
  } yield gc

  def generateGiftCardPurchase(customerId: Int, productContext: ProductContext)(implicit db: Database) : DbResultT[GiftCard] = for {
    order ← * <~ Orders.create(Order(state = Order.ManualHold, customerId = customerId, productContextId = productContext.id))
    orig  ← * <~ GiftCardOrders.create(GiftCardOrder(orderId = order.id))
    gc    ← * <~ GiftCards.create(GiftCard.buildLineItem(balance = nextGcBalance, originId = orig.id, currency = Currency.USD))
  } yield gc
}
