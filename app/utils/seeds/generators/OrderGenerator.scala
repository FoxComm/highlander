package utils.seeds.generators

import scala.concurrent.ExecutionContext.Implicits.global

import models.customer.Customer
import models.location.Addresses
import models.order.lineitems._
import models.order._
import models.payment.creditcard.{CreditCard, CreditCards}
import models.payment.giftcard._
import models.payment.storecredit._
import models.shipping.{Shipment, Shipments, ShippingMethods}
import models.Note
import models.product.{SimpleProductData, Mvp}
import models.objects.ObjectContext
import Order.{Cart, FraudHold, ManualHold, RemorseHold, Shipped}
import services.orders.OrderTotaler
import utils.db._
import utils.db.DbResultT._
import utils.seeds.ShipmentSeeds
import utils.time
import utils.aliases._
import java.time.Instant

import faker._
import scala.util.Random

import slick.driver.PostgresDriver.api._
import cats.implicits._
import failures.CreditCardFailures.CustomerHasNoCreditCard
import failures.CustomerFailures.CustomerHasNoDefaultAddress
import failures.ShippingMethodFailures.ShippingMethodIsNotFound
import services.inventory.InventoryAdjustmentManager

trait OrderGenerator extends ShipmentSeeds {

  def orderGenerators()(implicit db: DB) =
    List[(Int, ObjectContext, Seq[Int], GiftCard) ⇒  DbResultT[Order]](
      manualHoldOrder, manualHoldStoreCreditOrder, fraudHoldOrder, remorseHold,
      shippedOrderUsingGiftCard, shippedOrderUsingCreditCard)

  def cartGenerators()(implicit db: DB) =
    List[(Int, ObjectContext, Seq[Int], GiftCard) ⇒  DbResultT[Order]](
      cartOrderUsingGiftCard, cartOrderUsingCreditCard)

  def nextBalance = 1 + Random.nextInt(8000)

  def randomSubset[T](vals: Seq[T], maxSize: Int = 5) : Seq[T] = {
    require(vals.length > 0)
    val size = Math.max(Random.nextInt(Math.min(vals.length, maxSize)), 1)
    (1 to size).map {
      i ⇒  vals(i * Random.nextInt(vals.length) % vals.length)
    }.distinct
  }

  def generateOrders(customerId: Int, context: ObjectContext, skuIds: Seq[Int], giftCard: GiftCard) (implicit db: DB) : DbResultT[Unit] = {
    val cartFunctions = cartGenerators
    val orderFunctions = orderGenerators
    val cartIdx = Random.nextInt(cartFunctions.length)
    val cartFun = cartFunctions(cartIdx)
    for {
      _ ← * <~ cartFun(customerId, context, randomSubset(skuIds), giftCard)
      _ ← * <~ DbResultT.sequence((1 to 1 + Random.nextInt(4)).map { i ⇒
        val orderIdx = Random.nextInt(orderFunctions.length)
        val orderFun = orderFunctions(orderIdx)
        orderFun(customerId, context, randomSubset(skuIds), giftCard)
      })
    } yield {}
  }

  def manualHoldOrder(customerId: Customer#Id, context: ObjectContext, skuIds: Seq[Int], giftCard: GiftCard)(implicit db: DB): DbResultT[Order] = for {
    order  ← * <~ Orders.create(Order(state = ManualHold, customerId = customerId, contextId = context.id,
      placedAt = time.yesterday.toInstant.some))
    _      ← * <~ addProductsToOrder(skuIds, order.id, OrderLineItem.Pending)
    origin ← * <~ StoreCreditManuals.create(StoreCreditManual(adminId = 1, reasonId = 1))
    cc     ← * <~ getCc(customerId)
    op     ← * <~ OrderPayments.create(OrderPayment.build(cc).copy(orderId = order.id, amount = none))
    addr   ← * <~ getDefaultAddress(customerId)
    shipMethodIds ← * <~ ShippingMethods.map(_.id).result
    shipMethod     ← * <~ getShipMethod(1 + Random.nextInt(shipMethodIds.length))
    shipM ← * <~ OrderShippingMethods.create(OrderShippingMethod.build(order = order, method = shipMethod))
    _      ← * <~ OrderShippingAddresses.create(OrderShippingAddress.buildFromAddress(addr).copy(orderId = order.id))
    _      ← * <~ OrderTotaler.saveTotals(order)
    _      ← * <~ InventoryAdjustmentManager.orderPlaced(order)
  } yield order

  def manualHoldStoreCreditOrder(customerId: Customer#Id, context: ObjectContext, skuIds: Seq[Int], giftCard: GiftCard)(implicit db: DB): DbResultT[Order] = for {
    order  ← * <~ Orders.create(Order(state = ManualHold, customerId = customerId, contextId = context.id,
      placedAt = time.yesterday.toInstant.some))
    _      ← * <~ addProductsToOrder(skuIds, order.id, OrderLineItem.Pending)
    origin ← * <~ StoreCreditManuals.create(StoreCreditManual(adminId = 1, reasonId = 1))
    totals ← * <~ total(skuIds)
    sc     ← * <~ StoreCredits.create(StoreCredit(originId = origin.id, customerId = customerId, originalBalance = totals))
    op     ← * <~ OrderPayments.create(OrderPayment.build(sc).copy(orderId = order.id, amount = totals.some))
    _      ← * <~ StoreCredits.capture(sc, op.id.some, totals)
    addr   ← * <~ getDefaultAddress(customerId)
    shipMethodIds ← * <~ ShippingMethods.map(_.id).result
    shipMethod     ← * <~ getShipMethod(1 + Random.nextInt(shipMethodIds.length))
    shipM ← * <~ OrderShippingMethods.create(OrderShippingMethod.build(order = order, method = shipMethod))
    _      ← * <~ OrderShippingAddresses.create(OrderShippingAddress.buildFromAddress(addr).copy(orderId = order.id))
    _      ← * <~ OrderTotaler.saveTotals(order)
    _      ← * <~ InventoryAdjustmentManager.orderPlaced(order)
  } yield order

  def fraudHoldOrder(customerId: Customer#Id, context: ObjectContext, skuIds: Seq[Int], giftCard: GiftCard)(implicit db: DB): DbResultT[Order] = for {
    order  ← * <~ Orders.create(Order(state = FraudHold, customerId = customerId, contextId = context.id,
      placedAt = time.yesterday.toInstant.some))
    _      ← * <~ addProductsToOrder(skuIds, order.id, OrderLineItem.Pending)
    cc    ← * <~ getCc(customerId)
    op    ← * <~ OrderPayments.create(OrderPayment.build(cc).copy(orderId = order.id, amount = none))
    addr  ← * <~ getDefaultAddress(customerId)
    shipMethodIds ← * <~ ShippingMethods.map(_.id).result
    shipMethod     ← * <~ getShipMethod(1 + Random.nextInt(shipMethodIds.length))
    shipM ← * <~ OrderShippingMethods.create(OrderShippingMethod.build(order = order, method = shipMethod))
    _     ← * <~ OrderShippingAddresses.create(OrderShippingAddress.buildFromAddress(addr).copy(orderId = order.id))
    _     ← * <~ OrderTotaler.saveTotals(order)
    _     ← * <~ InventoryAdjustmentManager.orderPlaced(order)
  } yield order

  def remorseHold(customerId: Customer#Id, context: ObjectContext, skuIds: Seq[Int], giftCard: GiftCard)(implicit db: DB): DbResultT[Order] = for {
    randomHour     ← * <~ 1 + Random.nextInt(48)
    randomSeconds  ← * <~ randomHour * 3600
    order          ← * <~ Orders.create(Order(state = RemorseHold,
      customerId = customerId, contextId = context.id, remorsePeriodEnd = Instant.now.plusSeconds(randomSeconds.toLong).some,
      placedAt = time.yesterday.toInstant.some))
    _      ← * <~ addProductsToOrder(skuIds, order.id, OrderLineItem.Pending)
    cc    ← * <~ getCc(customerId)
    op    ← * <~ OrderPayments.create(OrderPayment.build(cc).copy(orderId = order.id, amount = none))
    addr  ← * <~ getDefaultAddress(customerId)
    shipMethodIds ← * <~ ShippingMethods.map(_.id).result
    shipMethod     ← * <~ getShipMethod(1 + Random.nextInt(shipMethodIds.length))
    shipM ← * <~ OrderShippingMethods.create(OrderShippingMethod.build(order = order, method = shipMethod))
    _     ← * <~ OrderShippingAddresses.create(OrderShippingAddress.buildFromAddress(addr).copy(orderId = order.id))
    _     ← * <~ OrderTotaler.saveTotals(order)
    _     ← * <~ InventoryAdjustmentManager.orderPlaced(order)
  } yield order

  def cartOrderUsingGiftCard(customerId: Customer#Id, context: ObjectContext, skuIds: Seq[Int], giftCard: GiftCard)(implicit db: DB): DbResultT[Order] = {
    val balance1 = nextBalance

    for {
      order  ← * <~ Orders.create(Order(state = Cart, customerId = customerId, contextId = context.id))
      _      ← * <~ addProductsToOrder(skuIds, orderId = order.id, OrderLineItem.Cart)
      cc     ← * <~ getCc(customerId)
      gc     ← * <~ GiftCards.mustFindById404(giftCard.id)
      totals ← * <~ total(skuIds)
      deductFromGc = deductAmount(gc.availableBalance, totals)
      _    ← * <~ generateOrderPayments(order, cc, gc, deductFromGc)
      // Authorize SC payments
      addr   ← * <~ getDefaultAddress(customerId)
      _      ← * <~ OrderShippingAddresses.create(OrderShippingAddress.buildFromAddress(addr).copy(orderId = order.id))
      _     ← * <~ OrderTotaler.saveTotals(order)
    } yield order
  }

  def cartOrderUsingCreditCard(customerId: Customer#Id, context: ObjectContext, skuIds: Seq[Int], giftCard: GiftCard)(implicit db: DB): DbResultT[Order] = for {
    order ← * <~ Orders.create(Order(state = Cart, customerId = customerId, contextId = context.id))
    _     ← * <~ addProductsToOrder(skuIds, order.id, OrderLineItem.Cart)
    cc    ← * <~ getCc(customerId)
    _     ← * <~ OrderPayments.create(OrderPayment.build(cc).copy(orderId = order.id, amount = none))
    addr  ← * <~ getDefaultAddress(customerId)
    _     ← * <~ OrderShippingAddresses.create(OrderShippingAddress.buildFromAddress(addr).copy(orderId = order.id))
    _     ← * <~ OrderTotaler.saveTotals(order)
  } yield order

  def shippedOrderUsingCreditCard(customerId: Customer#Id, context: ObjectContext, skuIds: Seq[Int], giftCard: GiftCard)
  (implicit db: DB): DbResultT[Order] = {
    for {
      shipMethodIds ← * <~ ShippingMethods.map(_.id).result
      shipMethod ← * <~ getShipMethod(1 + Random.nextInt(shipMethodIds.length))
      order ← * <~ Orders.create(Order(state = Shipped, customerId = customerId,
        contextId = context.id, placedAt = time.yesterday.toInstant.some))
      _     ← * <~ addProductsToOrder(skuIds, order.id, OrderLineItem.Shipped)
      cc    ← * <~ getCc(customerId) // TODO: auth
      op    ← * <~ OrderPayments.create(OrderPayment.build(cc).copy(orderId = order.id, amount = none))
      addr  ← * <~ getDefaultAddress(customerId)
      shipA ← * <~ OrderShippingAddresses.create(OrderShippingAddress.buildFromAddress(addr).copy(orderId = order.id))
      shipM ← * <~ OrderShippingMethods.create(OrderShippingMethod.build(order = order, method = shipMethod))
      _     ← * <~ OrderTotaler.saveTotals(order)
      _     ← * <~ Shipments.create(Shipment(orderId = order.id, orderShippingMethodId = shipM.id.some,
        shippingAddressId = shipA.id.some))
      _    ← * <~ InventoryAdjustmentManager.orderPlaced(order)
    } yield order
  }

  def shippedOrderUsingGiftCard(customerId: Customer#Id, context: ObjectContext, skuIds: Seq[Int], giftCard: GiftCard)
  (implicit db: DB): DbResultT[Order] = {
    for {
      shipMethodIds ← * <~ ShippingMethods.map(_.id).result
      shipMethod ← * <~ getShipMethod(1 + Random.nextInt(shipMethodIds.length))
      order ← * <~ Orders.create(Order(state = Shipped, customerId = customerId,
        contextId = context.id, placedAt = time.yesterday.toInstant.some))
      _     ← * <~ addProductsToOrder(skuIds, order.id, OrderLineItem.Shipped)
      gc     ← * <~ GiftCards.mustFindById404(giftCard.id)
      totals  ← * <~ total(skuIds)
      deductFromGc = deductAmount(gc.availableBalance, totals)
      cc    ← * <~ getCc(customerId) // TODO: auth
      _    ← * <~ generateOrderPayments(order, cc, gc, deductFromGc)
      gcPayments ← * <~ OrderPayments.findAllGiftCardsByOrderId(order.id).result
      _     ← * <~ authGiftCard(gcPayments)
      addr  ← * <~ getDefaultAddress(customerId)
      shipA ← * <~ OrderShippingAddresses.create(OrderShippingAddress.buildFromAddress(addr).copy(orderId = order.id))
      shipM ← * <~ OrderShippingMethods.create(OrderShippingMethod.build(order = order, method = shipMethod))
      _     ← * <~ OrderTotaler.saveTotals(order)
      _     ← * <~ Shipments.create(Shipment(orderId = order.id, orderShippingMethodId = shipM.id.some,
        shippingAddressId = shipA.id.some))
      _    ← * <~ InventoryAdjustmentManager.orderPlaced(order)
    } yield order
  }

  //TODO: Fix line item skus. bug if two or more contexts.
  def addProductsToOrder(skuIds: Seq[Int], orderId: Order#Id, state: OrderLineItem.State)(implicit db: DB):
  DbResultT[Unit] = for {
    liSkus ← * <~ OrderLineItemSkus.filter(_.id.inSet(skuIds)).result
    _ ← * <~ OrderLineItems.createAll(liSkus.seq.map { liSku ⇒
      OrderLineItem(orderId = orderId, originId = liSku.id, originType = OrderLineItem.SkuItem, state = state)
    })
  } yield {}

  def orderNotes: Seq[Note] = {
    def newNote(body: String) = Note(referenceId = 1, referenceType = Note.Order, storeAdminId = 1, body = body)
    (1 to Random.nextInt(4)) map { i ⇒  newNote(Lorem.sentence(Random.nextInt(5))) }
  }

  private def total(skuIds: Seq[Int])(implicit db: DB)  = for {
    prices ← * <~ DbResultT.sequence(skuIds.map(Mvp.getPrice))
    t ← * <~ prices.sum
  } yield t

  private def generateOrderPayments(order: Order, cc: CreditCard, gc: GiftCard, deductFromGc: Int) : DbResultT[Unit] = {
    if(gc.availableBalance > 0) for {
      op1    ← * <~ OrderPayments.create(OrderPayment.build(gc).copy(orderId = order.id, amount = deductFromGc.some))
      op2    ← * <~ OrderPayments.create(OrderPayment.build(cc).copy(orderId = order.id, amount = none))
    } yield {}
    else for {
      op    ← * <~ OrderPayments.create(OrderPayment.build(cc).copy(orderId = order.id, amount = none))
    } yield {}
  }

  private def getCc(customerId: Customer#Id)(implicit db: DB) =
    CreditCards.findDefaultByCustomerId(customerId).one
      .mustFindOr(CustomerHasNoCreditCard(customerId))

  private def getDefaultAddress(customerId: Customer#Id)(implicit db: DB) =
    Addresses.findAllByCustomerId(customerId).filter(_.isDefaultShipping).one
      .mustFindOr(CustomerHasNoDefaultAddress(customerId))

  private def getShipMethod(shipMethodId: Int)(implicit db: DB) =
    ShippingMethods.findActiveById(shipMethodId).one
      .mustFindOr(ShippingMethodIsNotFound(shipMethodId))

  private def authGiftCard(results: Seq[(OrderPayment, GiftCard)]):
  DbResultT[Seq[GiftCardAdjustment]] =
    DbResultT.sequence(results.map { case (pmt, m) ⇒ DbResultT(GiftCards.authOrderPayment(m, pmt)) })

  private def deductAmount(availableBalance: Int, totalCost: Int) : Int =
      Math.max(1, Math.min(
        Random.nextInt(
          Math.max(1, availableBalance)),
        Random.nextInt(
          Math.max(1, totalCost))))
}
