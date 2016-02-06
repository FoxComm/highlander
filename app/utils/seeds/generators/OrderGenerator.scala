package utils.seeds.generators

import scala.concurrent.ExecutionContext.Implicits.global

import models.{Addresses, CreditCards, Customer, GiftCard, GiftCardManual, GiftCardManuals, GiftCardOrder,
GiftCardOrders, GiftCards, Note, Notes, Order, OrderLineItem, OrderLineItemGiftCard, OrderLineItemGiftCards,
OrderLineItemSku, OrderLineItemSkus, OrderLineItems, OrderPayment, OrderPayments, OrderShippingAddress,
OrderShippingAddresses, OrderShippingMethod, OrderShippingMethods, ShippingMethods, Orders, Shipment, Shipments, Sku, StoreCredit,
StoreCreditManual, StoreCreditManuals, StoreCredits}
import models.GiftCard.buildAppeasement
import payloads.GiftCardCreateByCsr
import models.Order.{ManualHold, Cart, Shipped}
import utils.seeds.ShipmentSeeds

import utils.Money.Currency
import services.GeneralFailure
import services.orders.OrderTotaler
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._
import utils.DbResultT._
import utils.DbResultT.implicits._
import GeneratorUtils.randomString
import scala.util.Random
import utils.time
import cats.implicits._

import faker._;

trait OrderGenerator extends ShipmentSeeds {

  def orderGenerators()(implicit db: Database) = 
    List[(Int, Seq[Sku]) ⇒  DbResultT[Order]](
      generateOrder1, generateOrder2, generateOrder3, generateOrder4, generateOrder5) 

  def nextBalance = 1 + Random.nextInt(8000)
  def orderReferenceNum = {
    val base = new Base{}
    base.bothify("????####-##")
  }

  def generateOrder(customerId: Int, skus: Seq[Sku]) (implicit db: Database) : DbResultT[Order] = {
    val genFunctions = orderGenerators
    val genIdx = Random.nextInt(orderGenerators.length)
    val genFun = genFunctions(genIdx)
    genFun(customerId, skus)
  }

  def generateOrder1(customerId: Int, skus: Seq[Sku])(implicit db: Database): DbResultT[Order] = for {
    order ← * <~ Orders.create(Order(state = ManualHold, customerId = customerId, referenceNumber = orderReferenceNum))
    orig  ← * <~ GiftCardOrders.create(GiftCardOrder(orderId = order.id))
    gc    ← * <~ GiftCards.create(GiftCard.buildLineItem(balance = nextBalance, originId = orig.id, currency = Currency.USD))
    liGc  ← * <~ OrderLineItemGiftCards.create(OrderLineItemGiftCard(giftCardId = gc.id, orderId = order.id))
    _     ← * <~ OrderLineItems.create(OrderLineItem.buildGiftCard(order, liGc))
    addr  ← * <~ getDefaultAddress(customerId)
    _     ← * <~ OrderShippingAddresses.create(OrderShippingAddress.buildFromAddress(addr).copy(orderId = order.id))
    _     ← * <~ Notes.createAll(orderNotes.map(_.copy(referenceId = order.id)))
    _     ← * <~ OrderTotaler.saveTotals(order)
  } yield order

  def generateOrder2(customerId: Customer#Id, skus: Seq[Sku])(implicit db: Database): DbResultT[Order] = for {
    order  ← * <~ Orders.create(Order(state = ManualHold, customerId = customerId, referenceNumber = orderReferenceNum))
    _      ← * <~ addSkusToOrder(skus.map(_.id), order.id, OrderLineItem.Pending)
    origin ← * <~ StoreCreditManuals.create(StoreCreditManual(adminId = 1, reasonId = 1))
    totals = total(skus)
    sc     ← * <~ StoreCredits.create(StoreCredit(originId = origin.id, customerId = customerId, originalBalance = totals))
    op     ← * <~ OrderPayments.create(OrderPayment.build(sc).copy(orderId = order.id, amount = totals.some))
    _      ← * <~ StoreCredits.capture(sc, op.id.some, totals) 
    addr   ← * <~ getDefaultAddress(customerId)
    _      ← * <~ OrderShippingAddresses.create(OrderShippingAddress.buildFromAddress(addr).copy(orderId = order.id))
    _     ← * <~ OrderTotaler.saveTotals(order)
  } yield order

  def generateOrder3(customerId: Customer#Id, skus: Seq[Sku])(implicit db: Database): DbResultT[Order] = {
    val balance1 = nextBalance
    val balance2 = nextBalance

    for {
      order  ← * <~ Orders.create(Order(state = Cart, customerId = customerId, referenceNumber = orderReferenceNum))
      _      ← * <~ addSkusToOrder(skus.map(_.id), orderId = order.id, OrderLineItem.Cart)
      origin ← * <~ GiftCardManuals.create(GiftCardManual(adminId = 1, reasonId = 1))
      totals = total(skus)
      gc1    ← * <~ GiftCards.create(buildAppeasement(GiftCardCreateByCsr(balance = balance1, reasonId = 1), originId = origin.id))
      gc2    ← * <~ GiftCards.create(buildAppeasement(GiftCardCreateByCsr(balance = balance2, reasonId = 1), originId = origin.id))
      cc     ← * <~ getCc(customerId)
      _      ← * <~ OrderPayments.createAll(Seq(
        OrderPayment.build(gc1).copy(orderId = order.id, amount = balance1.some),
        OrderPayment.build(gc2).copy(orderId = order.id, amount = balance2.some),
        OrderPayment.build(cc).copy(orderId = order.id, amount = none)
      ))
      addr   ← * <~ getDefaultAddress(customerId)
      _      ← * <~ OrderShippingAddresses.create(OrderShippingAddress.buildFromAddress(addr).copy(orderId = order.id))
      _     ← * <~ OrderTotaler.saveTotals(order)
    } yield order
  }

  def generateOrder4(customerId: Customer#Id, skus: Seq[Sku])(implicit db: Database): DbResultT[Order] = for {
    order ← * <~ Orders.create(Order(state = Cart, customerId = customerId, referenceNumber = orderReferenceNum))
    _     ← * <~ addSkusToOrder(skus.map(_.id), order.id, OrderLineItem.Cart)
    cc    ← * <~ getCc(customerId)
    _     ← * <~ OrderPayments.create(OrderPayment.build(cc).copy(orderId = order.id, amount = none))
    addr  ← * <~ getDefaultAddress(customerId)
    _     ← * <~ OrderShippingAddresses.create(OrderShippingAddress.buildFromAddress(addr).copy(orderId = order.id))
    _     ← * <~ OrderTotaler.saveTotals(order)
  } yield order

  def generateOrder5(customerId: Customer#Id, skus: Seq[Sku])
  (implicit db: Database): DbResultT[Order] = { 
    for {
      shipMethodIds ← * <~ ShippingMethods.map(_.id).result
      shipMethodId ← * <~ shipMethodIds(Random.nextInt(shipMethodIds.length))
      order ← * <~ Orders.create(Order(state = Shipped,
        customerId = customerId, placedAt = Some(time.yesterday.toInstant), referenceNumber = orderReferenceNum))
      _     ← * <~ addSkusToOrder(skus.map(_.id), order.id, OrderLineItem.Shipped)
      cc    ← * <~ getCc(customerId) // TODO: auth
      op    ← * <~ OrderPayments.create(OrderPayment.build(cc).copy(orderId = order.id, amount = none))
      addr  ← * <~ getDefaultAddress(customerId)
      shipA ← * <~ OrderShippingAddresses.create(OrderShippingAddress.buildFromAddress(addr).copy(orderId = order.id))
      shipM ← * <~ OrderShippingMethods.create(OrderShippingMethod(orderId = order.id, shippingMethodId = shipMethodId))
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

  private def total(skus: Seq[Sku]) = skus.foldLeft(0)(_ + _.price)

  private def getCc(customerId: Customer#Id)(implicit db: Database) =
    CreditCards.findDefaultByCustomerId(customerId).one
      .mustFindOr(GeneralFailure(s"No cc found for customer with id=$customerId"))

  private def getDefaultAddress(customerId: Customer#Id)(implicit db: Database) =
    Addresses.findAllByCustomerId(customerId).filter(_.isDefaultShipping).one
      .mustFindOr(GeneralFailure(s"No default address found for customer with id =$customerId"))
}
