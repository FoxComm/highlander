package utils.seeds.generators

import scala.concurrent.ExecutionContext.Implicits.global

import models.customer.Customer
import models.inventory.Sku
import models.location.Addresses
import models.order.lineitems._
import models.order._
import models.payment.creditcard.{CreditCard, CreditCards}
import models.payment.giftcard._
import models.payment.storecredit._
import models.shipping.{Shipments, ShippingMethods, Shipment}
import models.{Note, Notes}
import GiftCard.buildAppeasement
import payloads.GiftCardCreateByCsr
import Order.{ManualHold, RemorseHold, FraudHold, Cart, Shipped}
import utils.seeds.ShipmentSeeds

import utils.Money.Currency
import services.{ShippingMethodIsNotFound, CustomerHasNoCreditCard, CustomerHasNoDefaultAddress, NotFoundFailure404}
import services.orders.OrderTotaler
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._
import utils.Slick.DbResult
import utils.DbResultT
import utils.DbResultT._
import utils.DbResultT.implicits._
import GeneratorUtils.randomString
import scala.util.Random
import utils.time
import cats.implicits._

import java.time.Instant
import faker._;

trait OrderGenerator extends ShipmentSeeds {

  def orderGenerators()(implicit db: Database) = 
    List[(Int, Seq[Sku], GiftCard) ⇒  DbResultT[Order]](
      manualHoldOrder, manualHoldStoreCreditOrder, fraudHoldOrder, remorseHold, 
      shippedOrderUsingGiftCard, shippedOrderUsingCreditCard) 

  def cartGenerators()(implicit db: Database) = 
    List[(Int, Seq[Sku], GiftCard) ⇒  DbResultT[Order]](
      cartOrderUsingGiftCard, cartOrderUsingCreditCard) 

  def nextBalance = 1 + Random.nextInt(8000)
  def orderReferenceNum = {
    val base = new Base{}
    base.bothify("????####-##")
  }

  def randomSubset[T](vals: Seq[T], maxSize: Int = 5) : Seq[T] = {
    require(vals.length > 0)
    val size = Math.max(Random.nextInt(Math.min(vals.length, maxSize)), 1)
    (1 to size).map { 
      i ⇒  vals(i * Random.nextInt(vals.length) % vals.length) 
    }.distinct
  }

  def generateOrders(customerId: Int, skus: Seq[Sku], giftCard: GiftCard) (implicit db: Database) : DbResultT[Unit] = {
    val cartFunctions = cartGenerators
    val orderFunctions = orderGenerators
    val cartIdx = Random.nextInt(cartFunctions.length)
    val cartFun = cartFunctions(cartIdx)
    for {
      _ ← * <~ cartFun(customerId, randomSubset(skus), giftCard)
      _ ← * <~ DbResultT.sequence((1 to 1 + Random.nextInt(4)).map { i ⇒ 
        val orderIdx = Random.nextInt(orderFunctions.length)
        val orderFun = orderFunctions(orderIdx)
        orderFun(customerId, randomSubset(skus), giftCard)
      })
    } yield {}
  }

  def manualHoldOrder(customerId: Customer#Id, skus: Seq[Sku], giftCard: GiftCard)(implicit db: Database): DbResultT[Order] = for {
    order  ← * <~ Orders.create(Order(state = ManualHold, customerId = customerId, referenceNumber = orderReferenceNum))
    _      ← * <~ addSkusToOrder(skus.map(_.id), order.id, OrderLineItem.Pending)
    origin ← * <~ StoreCreditManuals.create(StoreCreditManual(adminId = 1, reasonId = 1))
    totals = total(skus)
    cc     ← * <~ getCc(customerId) 
    op     ← * <~ OrderPayments.create(OrderPayment.build(cc).copy(orderId = order.id, amount = none))
    addr   ← * <~ getDefaultAddress(customerId)
    _      ← * <~ OrderShippingAddresses.create(OrderShippingAddress.buildFromAddress(addr).copy(orderId = order.id))
    _      ← * <~ OrderTotaler.saveTotals(order)
  } yield order

  def manualHoldStoreCreditOrder(customerId: Customer#Id, skus: Seq[Sku], giftCard: GiftCard)(implicit db: Database): DbResultT[Order] = for {
    order  ← * <~ Orders.create(Order(state = ManualHold, customerId = customerId, referenceNumber = orderReferenceNum))
    _      ← * <~ addSkusToOrder(skus.map(_.id), order.id, OrderLineItem.Pending)
    origin ← * <~ StoreCreditManuals.create(StoreCreditManual(adminId = 1, reasonId = 1))
    totals = total(skus)
    sc     ← * <~ StoreCredits.create(StoreCredit(originId = origin.id, customerId = customerId, originalBalance = totals))
    op     ← * <~ OrderPayments.create(OrderPayment.build(sc).copy(orderId = order.id, amount = totals.some))
    _      ← * <~ StoreCredits.capture(sc, op.id.some, totals) 
    addr   ← * <~ getDefaultAddress(customerId)
    _      ← * <~ OrderShippingAddresses.create(OrderShippingAddress.buildFromAddress(addr).copy(orderId = order.id))
    _      ← * <~ OrderTotaler.saveTotals(order)
  } yield order

  def fraudHoldOrder(customerId: Customer#Id, skus: Seq[Sku], giftCard: GiftCard)(implicit db: Database): DbResultT[Order] = for {
    order  ← * <~ Orders.create(Order(state = FraudHold, customerId = customerId, referenceNumber = orderReferenceNum))
    _      ← * <~ addSkusToOrder(skus.map(_.id), order.id, OrderLineItem.Pending)
    totals = total(skus)
    cc    ← * <~ getCc(customerId) 
    op    ← * <~ OrderPayments.create(OrderPayment.build(cc).copy(orderId = order.id, amount = none))
    addr  ← * <~ getDefaultAddress(customerId)
    _     ← * <~ OrderShippingAddresses.create(OrderShippingAddress.buildFromAddress(addr).copy(orderId = order.id))
    _     ← * <~ OrderTotaler.saveTotals(order)
  } yield order

  def remorseHold(customerId: Customer#Id, skus: Seq[Sku], giftCard: GiftCard)(implicit db: Database): DbResultT[Order] = for {
    randomHour     ← * <~ 1 + Random.nextInt(48)
    randomSeconds  ← * <~ randomHour * 3600
    order          ← * <~ Orders.create(Order(state = RemorseHold, customerId = customerId, 
      referenceNumber = orderReferenceNum, remorsePeriodEnd = Instant.now.plusSeconds(randomSeconds.toLong).some))
    _     ← * <~ addSkusToOrder(skus.map(_.id), order.id, OrderLineItem.Pending)
    cc    ← * <~ getCc(customerId) 
    op    ← * <~ OrderPayments.create(OrderPayment.build(cc).copy(orderId = order.id, amount = none))
    addr  ← * <~ getDefaultAddress(customerId)
    _     ← * <~ OrderShippingAddresses.create(OrderShippingAddress.buildFromAddress(addr).copy(orderId = order.id))
    _     ← * <~ OrderTotaler.saveTotals(order)
  } yield order

  def cartOrderUsingGiftCard(customerId: Customer#Id, skus: Seq[Sku], giftCard: GiftCard)(implicit db: Database): DbResultT[Order] = {
    val balance1 = nextBalance

    for {
      order  ← * <~ Orders.create(Order(state = Cart, customerId = customerId, referenceNumber = orderReferenceNum))
      _      ← * <~ addSkusToOrder(skus.map(_.id), orderId = order.id, OrderLineItem.Cart)
      cc     ← * <~ getCc(customerId)
      gc     ← * <~ GiftCards.mustFindById404(giftCard.id)
      totals = total(skus)
      deductFromGc = deductAmount(gc.availableBalance, totals)
      _    ← * <~ generateOrderPayments(order, cc, gc, deductFromGc)
      gcPayments ← * <~ OrderPayments.findAllGiftCardsByOrderId(order.id).result
      _     ← * <~ authGiftCard(gcPayments)
      // Authorize SC payments
      addr   ← * <~ getDefaultAddress(customerId)
      _      ← * <~ OrderShippingAddresses.create(OrderShippingAddress.buildFromAddress(addr).copy(orderId = order.id))
      _     ← * <~ OrderTotaler.saveTotals(order)
    } yield order
  }

  def cartOrderUsingCreditCard(customerId: Customer#Id, skus: Seq[Sku], giftCard: GiftCard)(implicit db: Database): DbResultT[Order] = for {
    order ← * <~ Orders.create(Order(state = Cart, customerId = customerId, referenceNumber = orderReferenceNum))
    _     ← * <~ addSkusToOrder(skus.map(_.id), order.id, OrderLineItem.Cart)
    cc    ← * <~ getCc(customerId)
    _     ← * <~ OrderPayments.create(OrderPayment.build(cc).copy(orderId = order.id, amount = none))
    addr  ← * <~ getDefaultAddress(customerId)
    _     ← * <~ OrderShippingAddresses.create(OrderShippingAddress.buildFromAddress(addr).copy(orderId = order.id))
    _     ← * <~ OrderTotaler.saveTotals(order)
  } yield order

  def shippedOrderUsingCreditCard(customerId: Customer#Id, skus: Seq[Sku], giftCard: GiftCard)
  (implicit db: Database): DbResultT[Order] = { 
    for {
      shipMethodIds ← * <~ ShippingMethods.map(_.id).result
      shipMethod ← * <~ getShipMethod(1 + Random.nextInt(shipMethodIds.length))
      shipMethodId ← * <~ shipMethod.id
      order ← * <~ Orders.create(Order(state = Shipped,
        customerId = customerId, placedAt = Some(time.yesterday.toInstant), referenceNumber = orderReferenceNum))
      _     ← * <~ addSkusToOrder(skus.map(_.id), order.id, OrderLineItem.Shipped)
      cc    ← * <~ getCc(customerId) // TODO: auth
      op    ← * <~ OrderPayments.create(OrderPayment.build(cc).copy(orderId = order.id, amount = none))
      addr  ← * <~ getDefaultAddress(customerId)
      shipA ← * <~ OrderShippingAddresses.create(OrderShippingAddress.buildFromAddress(addr).copy(orderId = order.id))
      shipM ← * <~ OrderShippingMethods.create(OrderShippingMethod.build(order = order, method = shipMethod))
      _     ← * <~ OrderTotaler.saveTotals(order)
      _     ← * <~ Shipments.create(Shipment(orderId = order.id, orderShippingMethodId = shipM.id.some,
        shippingAddressId = shipA.id.some))
    } yield order
  }

  def shippedOrderUsingGiftCard(customerId: Customer#Id, skus: Seq[Sku], giftCard: GiftCard)
  (implicit db: Database): DbResultT[Order] = { 
    for {
      shipMethodIds ← * <~ ShippingMethods.map(_.id).result
      shipMethod ← * <~ getShipMethod(1 + Random.nextInt(shipMethodIds.length))
      shipMethodId ← * <~ shipMethod.id
      order ← * <~ Orders.create(Order(state = Shipped,
        customerId = customerId, placedAt = Some(time.yesterday.toInstant), referenceNumber = orderReferenceNum))
      _  ← * <~ addSkusToOrder(skus.map(_.id), order.id, OrderLineItem.Shipped)
      gc     ← * <~ GiftCards.mustFindById404(giftCard.id)
      totals = total(skus)
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
    } yield order
  }

  def addSkusToOrder(skuIds: Seq[Sku#Id], orderId: Order#Id, state: OrderLineItem.State): DbResultT[Unit] = for {
    liSkus ← * <~ OrderLineItemSkus.filter(_.id.inSet(skuIds)).result
    _ ← * <~ OrderLineItems.createAll(liSkus.seq.map { liSku ⇒
      OrderLineItem(orderId = orderId, originId = liSku.id, originType = OrderLineItem.SkuItem, state = state)
    })
  } yield {}

  def orderNotes: Seq[Note] = {
    def newNote(body: String) = Note(referenceId = 1, referenceType = Note.Order, storeAdminId = 1, body = body)
    (1 to Random.nextInt(4)) map { i ⇒  newNote(Lorem.sentence(Random.nextInt(5))) } 
  }

  def generateOrderPayments(order: Order, cc: CreditCard, gc: GiftCard, deductFromGc: Int) : DbResultT[Unit] = {
    if(gc.availableBalance > 0) for {
      op1    ← * <~ OrderPayments.create(OrderPayment.build(gc).copy(orderId = order.id, amount = deductFromGc.some))
      op2    ← * <~ OrderPayments.create(OrderPayment.build(cc).copy(orderId = order.id, amount = none))
    } yield {}
    else for {
      op    ← * <~ OrderPayments.create(OrderPayment.build(cc).copy(orderId = order.id, amount = none))
    } yield {}
  }

  private def total(skus: Seq[Sku]) = skus.foldLeft(0)(_ + _.price)

  private def getCc(customerId: Customer#Id)(implicit db: Database) =
    CreditCards.findDefaultByCustomerId(customerId).one
      .mustFindOr(CustomerHasNoCreditCard(customerId))

  private def getDefaultAddress(customerId: Customer#Id)(implicit db: Database) =
    Addresses.findAllByCustomerId(customerId).filter(_.isDefaultShipping).one
      .mustFindOr(CustomerHasNoDefaultAddress(customerId))

  private def getShipMethod(shipMethodId: Int)(implicit db: Database) =
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
