package utils.seeds.generators

import java.time.Instant

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

import cats.implicits._
import failures.CreditCardFailures.CustomerHasNoCreditCard
import failures.CustomerFailures.CustomerHasNoDefaultAddress
import failures.NotFoundFailure400
import faker._
import models.Note
import models.account.Scope
import models.cord.Order._
import models.cord._
import models.cord.lineitems._
import models.inventory.Skus
import models.location.Addresses
import models.objects.ObjectContext
import models.payment.InStorePaymentStates
import models.payment.creditcard.CreditCardCharge.FullCapture
import models.payment.creditcard._
import models.payment.giftcard._
import models.payment.storecredit._
import models.product.Mvp
import models.shipping._
import services.carts.CartTotaler
import services.orders.OrderTotaler
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._
import utils.seeds.ShipmentSeeds
import utils.time

trait OrderGenerator extends ShipmentSeeds {

  def orderGenerators()(implicit db: DB, au: AU) =
    List[(Int, ObjectContext, Seq[Int], GiftCard) ⇒ DbResultT[Order]](manualHoldOrder,
                                                                      manualHoldStoreCreditOrder,
                                                                      fraudHoldOrder,
                                                                      remorseHold,
                                                                      shippedOrderUsingGiftCard,
                                                                      shippedOrderUsingCreditCard)

  def cartGenerators()(implicit db: DB, au: AU) =
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

  def generateOrders(accountId: Int, context: ObjectContext, skuIds: Seq[Int], giftCard: GiftCard)(
      implicit db: DB,
      au: AU): DbResultT[Unit] = {
    val cartFunctions  = cartGenerators
    val orderFunctions = orderGenerators
    val cartIdx        = Random.nextInt(cartFunctions.length)
    val cartFun        = cartFunctions(cartIdx)
    for {
      _ ← * <~ (1 to 1 + Random.nextInt(4)).map { i ⇒
           val orderIdx = Random.nextInt(orderFunctions.length)
           val orderFun = orderFunctions(orderIdx)
           orderFun(accountId, context, randomSubset(skuIds), giftCard)
         }
      _ ← * <~ cartFun(accountId, context, randomSubset(skuIds), giftCard)
    } yield {}
  }

  private val yesterday: Instant = time.yesterday.toInstant

  def manualHoldOrder(accountId: Int,
                      context: ObjectContext,
                      skuIds: Seq[Int],
                      giftCard: GiftCard)(implicit db: DB, au: AU): DbResultT[Order] =
    for {
      cart   ← * <~ Carts.create(Cart(accountId = accountId, scope = Scope.current))
      order  ← * <~ Orders.createFromCart(cart, context.id, None)
      order  ← * <~ Orders.update(order, order.copy(state = ManualHold, placedAt = yesterday))
      _      ← * <~ addProductsToOrder(skuIds, cart.refNum, OrderLineItem.Pending)
      origin ← * <~ StoreCreditManuals.create(StoreCreditManual(adminId = 1, reasonId = 1))
      cc     ← * <~ getCc(accountId)
      op ← * <~ OrderPayments.create(
              OrderPayment.build(cc).copy(cordRef = cart.refNum, amount = none))
      addr          ← * <~ getDefaultAddress(accountId)
      shipMethodIds ← * <~ ShippingMethods.map(_.id).result
      shipMethod    ← * <~ getShipMethod(1 + Random.nextInt(shipMethodIds.length))
      shipM ← * <~ OrderShippingMethods.create(
                 OrderShippingMethod.build(cordRef = cart.refNum, method = shipMethod))
      _ ← * <~ OrderShippingAddresses.create(
             OrderShippingAddress.buildFromAddress(addr).copy(cordRef = cart.refNum))
      _ ← * <~ OrderTotaler.saveTotals(cart, order)
    } yield order

  def manualHoldStoreCreditOrder(accountId: Int,
                                 context: ObjectContext,
                                 skuIds: Seq[Int],
                                 giftCard: GiftCard)(implicit db: DB, au: AU): DbResultT[Order] =
    for {
      cart   ← * <~ Carts.create(Cart(accountId = accountId, scope = Scope.current))
      order  ← * <~ Orders.createFromCart(cart, context.id, None)
      order  ← * <~ Orders.update(order, order.copy(state = ManualHold, placedAt = yesterday))
      _      ← * <~ addProductsToOrder(skuIds, cart.refNum, OrderLineItem.Pending)
      origin ← * <~ StoreCreditManuals.create(StoreCreditManual(adminId = 1, reasonId = 1))
      totals ← * <~ total(skuIds)
      sc ← * <~ StoreCredits.create(
              StoreCredit(scope = Scope.current,
                          originId = origin.id,
                          accountId = accountId,
                          originalBalance = totals))
      op ← * <~ OrderPayments.create(
              OrderPayment.build(sc).copy(cordRef = cart.refNum, amount = totals.some))
      _             ← * <~ StoreCredits.auth(sc, op.id, totals)
      _             ← * <~ StoreCredits.capture(sc, op.id, totals)
      addr          ← * <~ getDefaultAddress(accountId)
      shipMethodIds ← * <~ ShippingMethods.map(_.id).result
      shipMethod    ← * <~ getShipMethod(1 + Random.nextInt(shipMethodIds.length))
      shipM ← * <~ OrderShippingMethods.create(
                 OrderShippingMethod.build(cordRef = cart.refNum, method = shipMethod))
      _ ← * <~ OrderShippingAddresses.create(
             OrderShippingAddress.buildFromAddress(addr).copy(cordRef = cart.refNum))
      _ ← * <~ OrderTotaler.saveTotals(cart, order)
    } yield order

  def fraudHoldOrder(accountId: Int, context: ObjectContext, skuIds: Seq[Int], giftCard: GiftCard)(
      implicit db: DB,
      au: AU): DbResultT[Order] =
    for {
      cart  ← * <~ Carts.create(Cart(accountId = accountId, scope = Scope.current))
      order ← * <~ Orders.createFromCart(cart, context.id, None)
      order ← * <~ Orders.update(order, order.copy(state = FraudHold, placedAt = yesterday))
      _     ← * <~ addProductsToOrder(skuIds, cart.refNum, OrderLineItem.Pending)
      cc    ← * <~ getCc(accountId)
      op ← * <~ OrderPayments.create(
              OrderPayment.build(cc).copy(cordRef = cart.refNum, amount = none))
      addr          ← * <~ getDefaultAddress(accountId)
      shipMethodIds ← * <~ ShippingMethods.map(_.id).result
      shipMethod    ← * <~ getShipMethod(1 + Random.nextInt(shipMethodIds.length))
      shipM ← * <~ OrderShippingMethods.create(
                 OrderShippingMethod.build(cordRef = cart.refNum, method = shipMethod))
      _ ← * <~ OrderShippingAddresses.create(
             OrderShippingAddress.buildFromAddress(addr).copy(cordRef = cart.refNum))
      _ ← * <~ OrderTotaler.saveTotals(cart, order)
    } yield order

  def remorseHold(accountId: Int, context: ObjectContext, skuIds: Seq[Int], giftCard: GiftCard)(
      implicit db: DB,
      au: AU): DbResultT[Order] =
    for {
      randomHour    ← * <~ 1 + Random.nextInt(48)
      randomSeconds ← * <~ randomHour * 3600
      cart          ← * <~ Carts.create(Cart(accountId = accountId, scope = Scope.current))
      order         ← * <~ Orders.createFromCart(cart, context.id, None)
      order ← * <~ Orders.update(order,
                                 order.copy(state = RemorseHold,
                                            remorsePeriodEnd =
                                              Instant.now.plusSeconds(randomSeconds.toLong).some,
                                            placedAt = yesterday))
      _  ← * <~ addProductsToOrder(skuIds, cart.refNum, OrderLineItem.Pending)
      cc ← * <~ getCc(accountId)
      op ← * <~ OrderPayments.create(
              OrderPayment.build(cc).copy(cordRef = cart.refNum, amount = none))
      addr          ← * <~ getDefaultAddress(accountId)
      shipMethodIds ← * <~ ShippingMethods.map(_.id).result
      shipMethod    ← * <~ getShipMethod(1 + Random.nextInt(shipMethodIds.length))
      shipM ← * <~ OrderShippingMethods.create(
                 OrderShippingMethod.build(cordRef = cart.refNum, method = shipMethod))
      _ ← * <~ OrderShippingAddresses.create(
             OrderShippingAddress.buildFromAddress(addr).copy(cordRef = cart.refNum))
      _ ← * <~ OrderTotaler.saveTotals(cart, order)
    } yield order

  def cartOrderUsingGiftCard(accountId: Int,
                             context: ObjectContext,
                             skuIds: Seq[Int],
                             giftCard: GiftCard)(implicit db: DB, au: AU): DbResultT[Cart] = {

    for {
      cart   ← * <~ Carts.create(Cart(accountId = accountId, scope = Scope.current))
      _      ← * <~ addProductsToCart(skuIds, cart.refNum)
      cc     ← * <~ getCc(accountId)
      gc     ← * <~ GiftCards.mustFindById404(giftCard.id)
      totals ← * <~ total(skuIds)
      deductFromGc = deductAmount(gc.availableBalance, totals)
      _ ← * <~ generateCartPayments(cart, cc, gc, deductFromGc)
      // Authorize SC payments
      addr ← * <~ getDefaultAddress(accountId)
      _ ← * <~ OrderShippingAddresses.create(
             OrderShippingAddress.buildFromAddress(addr).copy(cordRef = cart.refNum))
      _ ← * <~ CartTotaler.saveTotals(cart)
    } yield cart
  }

  def cartOrderUsingCreditCard(accountId: Int,
                               context: ObjectContext,
                               skuIds: Seq[Int],
                               giftCard: GiftCard)(implicit db: DB, au: AU): DbResultT[Cart] =
    for {
      cart ← * <~ Carts.create(Cart(accountId = accountId, scope = Scope.current))
      _    ← * <~ addProductsToCart(skuIds, cart.refNum)
      cc   ← * <~ getCc(accountId)
      _ ← * <~ OrderPayments.create(
             OrderPayment.build(cc).copy(cordRef = cart.refNum, amount = none))
      addr ← * <~ getDefaultAddress(accountId)
      _ ← * <~ OrderShippingAddresses.create(
             OrderShippingAddress.buildFromAddress(addr).copy(cordRef = cart.refNum))
      _ ← * <~ CartTotaler.saveTotals(cart)
    } yield cart

  def shippedOrderUsingCreditCard(
      accountId: Int,
      context: ObjectContext,
      skuIds: Seq[Int],
      giftCard: GiftCard)(implicit db: DB, au: AU): DbResultT[Order] = {
    for {
      shipMethodIds ← * <~ ShippingMethods.map(_.id).result
      shipMethod    ← * <~ getShipMethod(1 + Random.nextInt(shipMethodIds.length))
      cart          ← * <~ Carts.create(Cart(accountId = accountId, scope = Scope.current))
      order         ← * <~ Orders.createFromCart(cart, context.id, None)
      order         ← * <~ Orders.update(order, order.copy(state = FulfillmentStarted))
      order         ← * <~ Orders.update(order, order.copy(state = Shipped, placedAt = yesterday))
      _             ← * <~ addProductsToOrder(skuIds, cart.refNum, OrderLineItem.Shipped)
      cc            ← * <~ getCc(accountId) // TODO: auth
      op ← * <~ OrderPayments.create(
              OrderPayment.build(cc).copy(cordRef = cart.refNum, amount = none))
      ccc ← * <~ CreditCardCharges.create(
               CreditCardCharge(creditCardId = cc.id,
                                orderPaymentId = op.id,
                                chargeId = s"${cc.id}_${op.id}",
                                state = FullCapture,
                                amount = op.amount.getOrElse(0)))
      addr ← * <~ getDefaultAddress(accountId)
      shipA ← * <~ OrderShippingAddresses.create(
                 OrderShippingAddress.buildFromAddress(addr).copy(cordRef = cart.refNum))
      shipM ← * <~ OrderShippingMethods.create(
                 OrderShippingMethod.build(cordRef = cart.refNum, method = shipMethod))
      _ ← * <~ OrderTotaler.saveTotals(cart, order)
      _ ← * <~ Shipments.create(
             Shipment(cordRef = cart.refNum,
                      orderShippingMethodId = shipM.id.some,
                      shippingAddressId = shipA.id.some))
    } yield order
  }

  def shippedOrderUsingGiftCard(accountId: Int,
                                context: ObjectContext,
                                skuIds: Seq[Int],
                                giftCard: GiftCard)(implicit db: DB, au: AU): DbResultT[Order] = {
    for {
      shipMethodIds ← * <~ ShippingMethods.map(_.id).result
      shipMethod    ← * <~ getShipMethod(1 + Random.nextInt(shipMethodIds.length))
      cart          ← * <~ Carts.create(Cart(accountId = accountId, scope = Scope.current))
      order         ← * <~ Orders.createFromCart(cart, context.id, None)
      order         ← * <~ Orders.update(order, order.copy(state = FulfillmentStarted))
      order         ← * <~ Orders.update(order, order.copy(state = Shipped, placedAt = yesterday))
      _             ← * <~ addProductsToOrder(skuIds, cart.refNum, OrderLineItem.Shipped)
      gc            ← * <~ GiftCards.mustFindById404(giftCard.id)
      totals        ← * <~ total(skuIds)
      deductFromGc = deductAmount(gc.availableBalance, totals)
      cc            ← * <~ getCc(accountId) // TODO: auth
      orderPayments ← * <~ generateOrderPayments(cart, order, cc, gc, deductFromGc)
      (gcPayment, ccPayment) = orderPayments
      gcPayments ← * <~ OrderPayments.findAllGiftCardsByCordRef(cart.refNum).result
      _          ← * <~ authGiftCard(gcPayments)
      _          ← * <~ generateCharges(Seq((cc, ccPayment)), gcPayment.toList.map(p ⇒ (gc, p)))
      addr       ← * <~ getDefaultAddress(accountId)
      shipA ← * <~ OrderShippingAddresses.create(
                 OrderShippingAddress.buildFromAddress(addr).copy(cordRef = cart.refNum))
      shipM ← * <~ OrderShippingMethods.create(
                 OrderShippingMethod.build(cordRef = cart.refNum, method = shipMethod))
      _ ← * <~ OrderTotaler.saveTotals(cart, order)
      _ ← * <~ Shipments.create(
             Shipment(cordRef = cart.refNum,
                      orderShippingMethodId = shipM.id.some,
                      shippingAddressId = shipA.id.some))
    } yield order
  }

  def addProductsToOrder(skuIds: Seq[Int], orderRef: String, state: OrderLineItem.State)(
      implicit db: DB): DbResultT[Unit] =
    for {
      skus ← * <~ Skus.filter(_.id inSet skuIds).result
      _ ← * <~ OrderLineItems.createAll(
             skus.map(
                 sku ⇒
                   OrderLineItem(cordRef = orderRef,
                                 skuId = sku.id,
                                 skuShadowId = sku.shadowId,
                                 state = state)))
    } yield {}

  def addProductsToCart(skuIds: Seq[Int], cartRef: String)(
      implicit db: DB): DbResultT[Seq[CartLineItem]] = {
    val itemsToInsert =
      skuIds.map(skuId ⇒ CartLineItem(cordRef = cartRef, skuId = skuId, attributes = None))
    CartLineItems.createAllReturningModels(itemsToInsert)
  }

  def orderNotes(implicit au: AU): Seq[Note] = {
    def newNote(body: String) =
      Note(referenceId = 1,
           referenceType = Note.Order,
           storeAdminId = 1,
           body = body,
           scope = Scope.current)
    (1 to Random.nextInt(4)) map { i ⇒
      newNote(Lorem.sentence(Random.nextInt(5)))
    }
  }

  private def total(skuIds: Seq[Int])(implicit db: DB) =
    for {
      prices ← * <~ skuIds.map(Mvp.getPrice)
      t      ← * <~ prices.sum
    } yield t

  def generateCharges(ccs: Seq[(CreditCard, OrderPayment)], gcs: Seq[(GiftCard, OrderPayment)])
    : DbResultT[(Seq[CreditCardCharge], Seq[GiftCardAdjustment])] =
    for {
      ccr ← * <~ ccs.map {
             case (cc, op) ⇒
               CreditCardCharges.create(
                   CreditCardCharge(creditCardId = cc.id,
                                    orderPaymentId = op.id,
                                    chargeId = s"${cc.id}_${op.id}",
                                    state = FullCapture,
                                    amount = op.amount.getOrElse(0)))
           }
      gcr ← * <~ gcs.map {
             case (gc, op) ⇒
               val amount = op.amount.getOrElse(0)
               GiftCardAdjustments.create(
                   GiftCardAdjustment(giftCardId = gc.id,
                                      orderPaymentId = op.id.some,
                                      credit = amount,
                                      debit = 0,
                                      availableBalance = gc.availableBalance - amount,
                                      state = InStorePaymentStates.Capture))
           }
    } yield (ccr, gcr)

  private def generateOrderPayments(
      cart: Cart,
      order: Order,
      cc: CreditCard,
      gc: GiftCard,
      deductFromGc: Int): DbResultT[(Option[OrderPayment], OrderPayment)] = {
    if (gc.availableBalance > 0)
      for {
        op1 ← * <~ OrderPayments.create(
                 OrderPayment.build(gc).copy(cordRef = cart.refNum, amount = deductFromGc.some))
        op2 ← * <~ OrderPayments.create(
                 OrderPayment.build(cc).copy(cordRef = cart.refNum, amount = none))
      } yield (op1.some, op2)
    else
      for {
        op ← * <~ OrderPayments.create(
                OrderPayment.build(cc).copy(cordRef = cart.refNum, amount = none))
      } yield (None, op)
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

  private def getCc(accountId: Int)(implicit db: DB) =
    CreditCards.findDefaultByAccountId(accountId).mustFindOneOr(CustomerHasNoCreditCard(accountId))

  private def getDefaultAddress(accountId: Int)(implicit db: DB) =
    Addresses
      .findAllByAccountId(accountId)
      .filter(_.isDefaultShipping)
      .mustFindOneOr(CustomerHasNoDefaultAddress(accountId))

  private def getShipMethod(shipMethodId: Int)(implicit db: DB) =
    ShippingMethods
      .findActiveById(shipMethodId)
      .mustFindOneOr(NotFoundFailure400(ShippingMethod, shipMethodId))

  private def authGiftCard(
      results: Seq[(OrderPayment, GiftCard)]): DbResultT[List[GiftCardAdjustment]] =
    DbResultT.sequenceJoiningFailures(results.map {
      case (pmt, m) ⇒ GiftCards.authOrderPayment(m, pmt)
    }.toList)

  private def deductAmount(availableBalance: Int, totalCost: Int): Int =
    Math.max(1,
             Math.min(Random.nextInt(Math.max(1, availableBalance)),
                      Random.nextInt(Math.max(1, totalCost))))
}
