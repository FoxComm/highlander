package utils.seeds.generators

import java.time.Instant

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

import cats.implicits._
import failures.CreditCardFailures.CustomerHasNoCreditCard
import failures.CustomerFailures.CustomerHasNoDefaultAddress
import failures.ShippingMethodFailures.ShippingMethodIsNotFound
import faker._
import models.Note
import models.customer.Customer
import models.location.Addresses
import models.objects.ObjectContext
import models.cord.Order.{FraudHold, ManualHold, RemorseHold, Shipped}
import models.cord._
import models.cord.lineitems._
import models.payment.creditcard.{CreditCard, CreditCards}
import models.payment.giftcard._
import models.payment.storecredit._
import models.product.Mvp
import models.shipping.{Shipment, Shipments, ShippingMethods}
import services.orders.OrderTotaler
import services.carts.CartTotaler
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._
import utils.seeds.ShipmentSeeds
import utils.time

trait OrderGenerator extends ShipmentSeeds {

  def orderGenerators()(implicit db: DB) =
    List[(Int, ObjectContext, Seq[Int], GiftCard) ⇒ DbResultT[Order]](manualHoldOrder,
                                                                      manualHoldStoreCreditOrder,
                                                                      fraudHoldOrder,
                                                                      remorseHold,
                                                                      shippedOrderUsingGiftCard,
                                                                      shippedOrderUsingCreditCard)

  def cartGenerators()(implicit db: DB) =
    List[(Int, ObjectContext, Seq[Int], GiftCard) ⇒ DbResultT[Cart]](cartOrderUsingGiftCard,
                                                                     cartOrderUsingCreditCard)

  def nextBalance = 1 + Random.nextInt(8000)

  def randomSubset[T](vals: Seq[T], maxSize: Int = 5): Seq[T] = {
    require(vals.nonEmpty)
    val size = Math.max(Random.nextInt(Math.min(vals.length, maxSize)), 1)
    (1 to size).map { i ⇒
      vals(i * Random.nextInt(vals.length) % vals.length)
    }.distinct
  }

  def generateOrders(customerId: Int,
                     context: ObjectContext,
                     skuIds: Seq[Int],
                     giftCard: GiftCard)(implicit db: DB): DbResultT[Unit] = {
    val cartFunctions  = cartGenerators
    val orderFunctions = orderGenerators
    val cartIdx        = Random.nextInt(cartFunctions.length)
    val cartFun        = cartFunctions(cartIdx)
    for {
      _ ← * <~ (1 to 1 + Random.nextInt(4)).map { i ⇒
           val orderIdx = Random.nextInt(orderFunctions.length)
           val orderFun = orderFunctions(orderIdx)
           orderFun(customerId, context, randomSubset(skuIds), giftCard)
         }
      _ ← * <~ cartFun(customerId, context, randomSubset(skuIds), giftCard)
    } yield {}
  }

  def manualHoldOrder(customerId: Customer#Id,
                      context: ObjectContext,
                      skuIds: Seq[Int],
                      giftCard: GiftCard)(implicit db: DB): DbResultT[Order] =
    for {
      cart ← * <~ Carts.create(Cart(customerId = customerId))
      order ← * <~ Orders.create(
                 cart
                   .toOrder(context.id)
                   .copy(state = ManualHold, placedAt = time.yesterday.toInstant))
      _      ← * <~ addProductsToOrder(skuIds, cart.refNum, OrderLineItem.Pending)
      origin ← * <~ StoreCreditManuals.create(StoreCreditManual(adminId = 1, reasonId = 1))
      cc     ← * <~ getCc(customerId)
      op ← * <~ OrderPayments.create(
              OrderPayment.build(cc).copy(cordRef = cart.refNum, amount = none))
      addr          ← * <~ getDefaultAddress(customerId)
      shipMethodIds ← * <~ ShippingMethods.map(_.id).result
      shipMethod    ← * <~ getShipMethod(1 + Random.nextInt(shipMethodIds.length))
      shipM ← * <~ OrderShippingMethods.create(
                 OrderShippingMethod.build(cordRef = cart.refNum, method = shipMethod))
      _ ← * <~ OrderShippingAddresses.create(
             OrderShippingAddress.buildFromAddress(addr).copy(cordRef = cart.refNum))
      _ ← * <~ OrderTotaler.saveTotals(cart, order)
      _ ← * <~ Carts.forceUpdate(cart, cart.copy(isActive = false))
    } yield order

  def manualHoldStoreCreditOrder(customerId: Customer#Id,
                                 context: ObjectContext,
                                 skuIds: Seq[Int],
                                 giftCard: GiftCard)(implicit db: DB): DbResultT[Order] =
    for {
      cart ← * <~ Carts.create(Cart(customerId = customerId))
      order ← * <~ Orders.create(
                 cart
                   .toOrder(context.id)
                   .copy(state = ManualHold, placedAt = time.yesterday.toInstant))
      _      ← * <~ addProductsToOrder(skuIds, cart.refNum, OrderLineItem.Pending)
      origin ← * <~ StoreCreditManuals.create(StoreCreditManual(adminId = 1, reasonId = 1))
      totals ← * <~ total(skuIds)
      sc ← * <~ StoreCredits.create(
              StoreCredit(originId = origin.id, customerId = customerId, originalBalance = totals))
      op ← * <~ OrderPayments.create(
              OrderPayment.build(sc).copy(cordRef = cart.refNum, amount = totals.some))
      _             ← * <~ StoreCredits.capture(sc, op.id.some, totals)
      addr          ← * <~ getDefaultAddress(customerId)
      shipMethodIds ← * <~ ShippingMethods.map(_.id).result
      shipMethod    ← * <~ getShipMethod(1 + Random.nextInt(shipMethodIds.length))
      shipM ← * <~ OrderShippingMethods.create(
                 OrderShippingMethod.build(cordRef = cart.refNum, method = shipMethod))
      _ ← * <~ OrderShippingAddresses.create(
             OrderShippingAddress.buildFromAddress(addr).copy(cordRef = cart.refNum))
      _ ← * <~ OrderTotaler.saveTotals(cart, order)
      _ ← * <~ Carts.forceUpdate(cart, cart.copy(isActive = false))
    } yield order

  def fraudHoldOrder(customerId: Customer#Id,
                     context: ObjectContext,
                     skuIds: Seq[Int],
                     giftCard: GiftCard)(implicit db: DB): DbResultT[Order] =
    for {
      cart ← * <~ Carts.create(Cart(customerId = customerId))
      order ← * <~ Orders.create(
                 cart
                   .toOrder(context.id)
                   .copy(state = FraudHold, placedAt = time.yesterday.toInstant))
      _  ← * <~ addProductsToOrder(skuIds, cart.refNum, OrderLineItem.Pending)
      cc ← * <~ getCc(customerId)
      op ← * <~ OrderPayments.create(
              OrderPayment.build(cc).copy(cordRef = cart.refNum, amount = none))
      addr          ← * <~ getDefaultAddress(customerId)
      shipMethodIds ← * <~ ShippingMethods.map(_.id).result
      shipMethod    ← * <~ getShipMethod(1 + Random.nextInt(shipMethodIds.length))
      shipM ← * <~ OrderShippingMethods.create(
                 OrderShippingMethod.build(cordRef = cart.refNum, method = shipMethod))
      _ ← * <~ OrderShippingAddresses.create(
             OrderShippingAddress.buildFromAddress(addr).copy(cordRef = cart.refNum))
      _ ← * <~ OrderTotaler.saveTotals(cart, order)
      _ ← * <~ Carts.forceUpdate(cart, cart.copy(isActive = false))
    } yield order

  def remorseHold(customerId: Customer#Id,
                  context: ObjectContext,
                  skuIds: Seq[Int],
                  giftCard: GiftCard)(implicit db: DB): DbResultT[Order] =
    for {
      randomHour    ← * <~ 1 + Random.nextInt(48)
      randomSeconds ← * <~ randomHour * 3600
      cart          ← * <~ Carts.create(Cart(customerId = customerId))
      order ← * <~ Orders.create(
                 cart
                   .toOrder(context.id)
                   .copy(state = RemorseHold,
                         remorsePeriodEnd = Instant.now.plusSeconds(randomSeconds.toLong).some,
                         placedAt = time.yesterday.toInstant))
      _  ← * <~ addProductsToOrder(skuIds, cart.refNum, OrderLineItem.Pending)
      cc ← * <~ getCc(customerId)
      op ← * <~ OrderPayments.create(
              OrderPayment.build(cc).copy(cordRef = cart.refNum, amount = none))
      addr          ← * <~ getDefaultAddress(customerId)
      shipMethodIds ← * <~ ShippingMethods.map(_.id).result
      shipMethod    ← * <~ getShipMethod(1 + Random.nextInt(shipMethodIds.length))
      shipM ← * <~ OrderShippingMethods.create(
                 OrderShippingMethod.build(cordRef = cart.refNum, method = shipMethod))
      _ ← * <~ OrderShippingAddresses.create(
             OrderShippingAddress.buildFromAddress(addr).copy(cordRef = cart.refNum))
      _ ← * <~ OrderTotaler.saveTotals(cart, order)
      _ ← * <~ Carts.forceUpdate(cart, cart.copy(isActive = false))
    } yield order

  def cartOrderUsingGiftCard(customerId: Customer#Id,
                             context: ObjectContext,
                             skuIds: Seq[Int],
                             giftCard: GiftCard)(implicit db: DB): DbResultT[Cart] = {

    for {
      cart   ← * <~ Carts.create(Cart(customerId = customerId))
      _      ← * <~ addProductsToOrder(skuIds, orderRef = cart.refNum, OrderLineItem.Cart)
      cc     ← * <~ getCc(customerId)
      gc     ← * <~ GiftCards.mustFindById404(giftCard.id)
      totals ← * <~ total(skuIds)
      deductFromGc = deductAmount(gc.availableBalance, totals)
      _ ← * <~ generateCartPayments(cart, cc, gc, deductFromGc)
      // Authorize SC payments
      addr ← * <~ getDefaultAddress(customerId)
      _ ← * <~ OrderShippingAddresses.create(
             OrderShippingAddress.buildFromAddress(addr).copy(cordRef = cart.refNum))
      _ ← * <~ CartTotaler.saveTotals(cart)
    } yield cart
  }

  def cartOrderUsingCreditCard(customerId: Customer#Id,
                               context: ObjectContext,
                               skuIds: Seq[Int],
                               giftCard: GiftCard)(implicit db: DB): DbResultT[Cart] =
    for {
      cart ← * <~ Carts.create(Cart(customerId = customerId))
      _    ← * <~ addProductsToOrder(skuIds, cart.refNum, OrderLineItem.Cart)
      cc   ← * <~ getCc(customerId)
      _ ← * <~ OrderPayments.create(
             OrderPayment.build(cc).copy(cordRef = cart.refNum, amount = none))
      addr ← * <~ getDefaultAddress(customerId)
      _ ← * <~ OrderShippingAddresses.create(
             OrderShippingAddress.buildFromAddress(addr).copy(cordRef = cart.refNum))
      _ ← * <~ CartTotaler.saveTotals(cart)
    } yield cart

  def shippedOrderUsingCreditCard(customerId: Customer#Id,
                                  context: ObjectContext,
                                  skuIds: Seq[Int],
                                  giftCard: GiftCard)(implicit db: DB): DbResultT[Order] = {
    for {
      shipMethodIds ← * <~ ShippingMethods.map(_.id).result
      shipMethod    ← * <~ getShipMethod(1 + Random.nextInt(shipMethodIds.length))
      cart          ← * <~ Carts.create(Cart(customerId = customerId))
      order ← * <~ Orders.create(
                 cart
                   .toOrder(context.id)
                   .copy(state = Shipped, placedAt = time.yesterday.toInstant))
      _  ← * <~ addProductsToOrder(skuIds, cart.refNum, OrderLineItem.Shipped)
      cc ← * <~ getCc(customerId) // TODO: auth
      op ← * <~ OrderPayments.create(
              OrderPayment.build(cc).copy(cordRef = cart.refNum, amount = none))
      addr ← * <~ getDefaultAddress(customerId)
      shipA ← * <~ OrderShippingAddresses.create(
                 OrderShippingAddress.buildFromAddress(addr).copy(cordRef = cart.refNum))
      shipM ← * <~ OrderShippingMethods.create(
                 OrderShippingMethod.build(cordRef = cart.refNum, method = shipMethod))
      _ ← * <~ OrderTotaler.saveTotals(cart, order)
      _ ← * <~ Shipments.create(
             Shipment(cordRef = cart.refNum,
                      orderShippingMethodId = shipM.id.some,
                      shippingAddressId = shipA.id.some))
      _ ← * <~ Carts.forceUpdate(cart, cart.copy(isActive = false))
    } yield order
  }

  def shippedOrderUsingGiftCard(customerId: Customer#Id,
                                context: ObjectContext,
                                skuIds: Seq[Int],
                                giftCard: GiftCard)(implicit db: DB): DbResultT[Order] = {
    for {
      shipMethodIds ← * <~ ShippingMethods.map(_.id).result
      shipMethod    ← * <~ getShipMethod(1 + Random.nextInt(shipMethodIds.length))
      cart          ← * <~ Carts.create(Cart(customerId = customerId))
      order ← * <~ Orders.create(
                 cart
                   .toOrder(context.id)
                   .copy(state = Shipped, placedAt = time.yesterday.toInstant))
      _      ← * <~ addProductsToOrder(skuIds, cart.refNum, OrderLineItem.Shipped)
      gc     ← * <~ GiftCards.mustFindById404(giftCard.id)
      totals ← * <~ total(skuIds)
      deductFromGc = deductAmount(gc.availableBalance, totals)
      cc         ← * <~ getCc(customerId) // TODO: auth
      _          ← * <~ generateOrderPayments(cart, order, cc, gc, deductFromGc)
      gcPayments ← * <~ OrderPayments.findAllGiftCardsByOrderRef(cart.refNum).result
      _          ← * <~ authGiftCard(gcPayments)
      addr       ← * <~ getDefaultAddress(customerId)
      shipA ← * <~ OrderShippingAddresses.create(
                 OrderShippingAddress.buildFromAddress(addr).copy(cordRef = cart.refNum))
      shipM ← * <~ OrderShippingMethods.create(
                 OrderShippingMethod.build(cordRef = cart.refNum, method = shipMethod))
      _ ← * <~ OrderTotaler.saveTotals(cart, order)
      _ ← * <~ Shipments.create(
             Shipment(cordRef = cart.refNum,
                      orderShippingMethodId = shipM.id.some,
                      shippingAddressId = shipA.id.some))
      _ ← * <~ Carts.forceUpdate(cart, cart.copy(isActive = false))
    } yield order
  }

  //TODO: Fix line item skus. bug if two or more contexts.
  def addProductsToOrder(skuIds: Seq[Int], orderRef: String, state: OrderLineItem.State)(
      implicit db: DB): DbResultT[Unit] =
    for {
      liSkus ← * <~ OrderLineItemSkus.filter(_.id.inSet(skuIds)).result
      _ ← * <~ OrderLineItems.createAll(liSkus.seq.map { liSku ⇒
           OrderLineItem(cordRef = orderRef,
                         originId = liSku.id,
                         originType = OrderLineItem.SkuItem,
                         state = state)
         })
    } yield {}

  def orderNotes: Seq[Note] = {
    def newNote(body: String) =
      Note(referenceId = 1, referenceType = Note.Order, storeAdminId = 1, body = body)
    (1 to Random.nextInt(4)) map { i ⇒
      newNote(Lorem.sentence(Random.nextInt(5)))
    }
  }

  private def total(skuIds: Seq[Int])(implicit db: DB) =
    for {
      prices ← * <~ skuIds.map(Mvp.getPrice)
      t      ← * <~ prices.sum
    } yield t

  private def generateOrderPayments(cart: Cart,
                                    order: Order,
                                    cc: CreditCard,
                                    gc: GiftCard,
                                    deductFromGc: Int): DbResultT[Unit] = {
    if (gc.availableBalance > 0)
      for {
        op1 ← * <~ OrderPayments.create(
                 OrderPayment.build(gc).copy(cordRef = cart.refNum, amount = deductFromGc.some))
        op2 ← * <~ OrderPayments.create(
                 OrderPayment.build(cc).copy(cordRef = cart.refNum, amount = none))
      } yield {} else
      for {
        op ← * <~ OrderPayments.create(
                OrderPayment.build(cc).copy(cordRef = cart.refNum, amount = none))
      } yield {}
  }

  private def generateCartPayments(cart: Cart,
                                   cc: CreditCard,
                                   gc: GiftCard,
                                   deductFromGc: Int): DbResultT[Unit] = {
    if (gc.availableBalance > 0)
      for {
        op1 ← * <~ OrderPayments.create(
                 OrderPayment.build(gc).copy(cordRef = cart.refNum, amount = deductFromGc.some))
        op2 ← * <~ OrderPayments.create(
                 OrderPayment.build(cc).copy(cordRef = cart.refNum, amount = none))
      } yield {} else
      for {
        op ← * <~ OrderPayments.create(
                OrderPayment.build(cc).copy(cordRef = cart.refNum, amount = none))
      } yield {}
  }

  private def getCc(customerId: Customer#Id)(implicit db: DB) =
    CreditCards
      .findDefaultByCustomerId(customerId)
      .mustFindOneOr(CustomerHasNoCreditCard(customerId))

  private def getDefaultAddress(customerId: Customer#Id)(implicit db: DB) =
    Addresses
      .findAllByCustomerId(customerId)
      .filter(_.isDefaultShipping)
      .mustFindOneOr(CustomerHasNoDefaultAddress(customerId))

  private def getShipMethod(shipMethodId: Int)(implicit db: DB) =
    ShippingMethods
      .findActiveById(shipMethodId)
      .mustFindOneOr(ShippingMethodIsNotFound(shipMethodId))

  private def authGiftCard(
      results: Seq[(OrderPayment, GiftCard)]): DbResultT[Seq[GiftCardAdjustment]] =
    DbResultT.sequence(results.map { case (pmt, m) ⇒ GiftCards.authOrderPayment(m, pmt) })

  private def deductAmount(availableBalance: Int, totalCost: Int): Int =
    Math.max(1,
             Math.min(Random.nextInt(Math.max(1, availableBalance)),
                      Random.nextInt(Math.max(1, totalCost))))
}
