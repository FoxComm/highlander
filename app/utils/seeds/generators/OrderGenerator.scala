package utils.seeds.generators

import scala.concurrent.ExecutionContext.Implicits.global

import models.{Addresses, CreditCards, Customer, GiftCard, GiftCardManual, 
  GiftCardManuals, GiftCardOrder, GiftCardOrders, GiftCards, Note, Notes, Order, 
  OrderLineItem, OrderLineItemGiftCard, OrderLineItemGiftCards, OrderLineItemSku, 
  OrderLineItemSkus, OrderLineItems, OrderPayment, OrderPayments, OrderShippingAddress,
  OrderShippingAddresses, OrderShippingMethod, OrderShippingMethods, ShippingMethods, 
  Orders, Shipment, Shipments, StoreCredit, StoreCreditManual, StoreCreditManuals, 
  StoreCredits}
import GeneratorUtils.randomString
import models.GiftCard.buildAppeasement
import models.Order.{ManualHold, Cart, Shipped}
import models.product.{ProductContext, SimpleProductData, Mvp}

import payloads.GiftCardCreateByCsr

import services.orders.OrderTotaler
import services.{CustomerHasNoCreditCard, CustomerHasNoDefaultAddress, NotFoundFailure404}

import utils.DbResultT
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Money.Currency
import utils.Slick.implicits._
import utils.seeds.ShipmentSeeds
import utils.time

import cats.implicits._
import faker._;
import scala.util.Random
import slick.driver.PostgresDriver.api._

trait OrderGenerator extends ShipmentSeeds {

  def orderGenerators()(implicit db: Database) = 
    List[(Int, ProductContext, Seq[SimpleProductData]) ⇒  DbResultT[Order]](
      generateOrder1, generateOrder2, generateOrder3, generateOrder4, generateOrder5) 

  def nextBalance = 1 + Random.nextInt(8000)
  def orderReferenceNum = {
    val base = new Base{}
    base.bothify("????####-##")
  }

  def generateOrder(customerId: Int, productContext: ProductContext, products: Seq[SimpleProductData]) (implicit db: Database) : DbResultT[Order] = {
    val genFunctions = orderGenerators
    val genIdx = Random.nextInt(orderGenerators.length)
    val genFun = genFunctions(genIdx)
    genFun(customerId, productContext, products)
  }

  def generateOrder1(customerId: Customer#Id, productContext: ProductContext, products: Seq[SimpleProductData])(implicit db: Database): DbResultT[Order] = for {
    order ← * <~ Orders.create(Order(state = ManualHold, customerId = customerId, productContextId = productContext.id, referenceNumber = orderReferenceNum))
    orig  ← * <~ GiftCardOrders.create(GiftCardOrder(orderId = order.id))
    gc    ← * <~ GiftCards.create(GiftCard.buildLineItem(balance = nextBalance, originId = orig.id, currency = Currency.USD))
    liGc  ← * <~ OrderLineItemGiftCards.create(OrderLineItemGiftCard(giftCardId = gc.id, orderId = order.id))
    _     ← * <~ OrderLineItems.create(OrderLineItem.buildGiftCard(order, liGc))
    addr  ← * <~ getDefaultAddress(customerId)
    _     ← * <~ OrderShippingAddresses.create(OrderShippingAddress.buildFromAddress(addr).copy(orderId = order.id))
    _     ← * <~ Notes.createAll(orderNotes.map(_.copy(referenceId = order.id)))
    _     ← * <~ OrderTotaler.saveTotals(order)
  } yield order

  def generateOrder2(customerId: Customer#Id, productContext: ProductContext, products: Seq[SimpleProductData])(implicit db: Database): DbResultT[Order] = for {
    order  ← * <~ Orders.create(Order(state = ManualHold, customerId = customerId, productContextId = productContext.id, referenceNumber = orderReferenceNum))
    _      ← * <~ addProductsToOrder(products, order.id, OrderLineItem.Pending)
    origin ← * <~ StoreCreditManuals.create(StoreCreditManual(adminId = 1, reasonId = 1))
    totals ← * <~ total(products)
    sc     ← * <~ StoreCredits.create(StoreCredit(originId = origin.id, customerId = customerId, originalBalance = totals))
    op     ← * <~ OrderPayments.create(OrderPayment.build(sc).copy(orderId = order.id, amount = totals.some))
    _      ← * <~ StoreCredits.capture(sc, op.id.some, totals) 
    addr   ← * <~ getDefaultAddress(customerId)
    _      ← * <~ OrderShippingAddresses.create(OrderShippingAddress.buildFromAddress(addr).copy(orderId = order.id))
    _     ← * <~ OrderTotaler.saveTotals(order)
  } yield order

  def generateOrder3(customerId: Customer#Id, productContext: ProductContext, products: Seq[SimpleProductData])(implicit db: Database): DbResultT[Order] = {
    val balance1 = nextBalance
    val balance2 = nextBalance

    for {
      order  ← * <~ Orders.create(Order(state = Cart, customerId = customerId, productContextId = productContext.id, referenceNumber = orderReferenceNum))
      _      ← * <~ addProductsToOrder(products, orderId = order.id, OrderLineItem.Cart)
      origin ← * <~ GiftCardManuals.create(GiftCardManual(adminId = 1, reasonId = 1))
      totals ← * <~ total(products)
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

  def generateOrder4(customerId: Customer#Id, productContext: ProductContext, products: Seq[SimpleProductData])(implicit db: Database): DbResultT[Order] = for {
    order ← * <~ Orders.create(Order(state = Cart, customerId = customerId, productContextId = productContext.id, referenceNumber = orderReferenceNum))
    _     ← * <~ addProductsToOrder(products, order.id, OrderLineItem.Cart)
    cc    ← * <~ getCc(customerId)
    _     ← * <~ OrderPayments.create(OrderPayment.build(cc).copy(orderId = order.id, amount = none))
    addr  ← * <~ getDefaultAddress(customerId)
    _     ← * <~ OrderShippingAddresses.create(OrderShippingAddress.buildFromAddress(addr).copy(orderId = order.id))
    _     ← * <~ OrderTotaler.saveTotals(order)
  } yield order

  def generateOrder5(customerId: Customer#Id, productContext: ProductContext, products: Seq[SimpleProductData])
  (implicit db: Database): DbResultT[Order] = { 
    for {
      shipMethodIds ← * <~ ShippingMethods.map(_.id).result
      shipMethodId ← * <~ shipMethodIds(Random.nextInt(shipMethodIds.length))
      order ← * <~ Orders.create(Order(state = Shipped, customerId = customerId, 
        productContextId = productContext.id, placedAt = Some(time.yesterday.toInstant), 
        referenceNumber = orderReferenceNum))
      _     ← * <~ addProductsToOrder(products, order.id, OrderLineItem.Shipped)
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

  //TODO: Fix line item skus. bug if two or more contexts.
  def addProductsToOrder(products: Seq[SimpleProductData], orderId: Order#Id, state: OrderLineItem.State)(implicit db: Database): 
  DbResultT[Unit] = for {
    liSkus ← * <~ OrderLineItemSkus.filter(_.id.inSet(products.map(_.skuId))).result
    _ ← * <~ OrderLineItems.createAll(liSkus.seq.map { liSku ⇒
      OrderLineItem(orderId = orderId, originId = liSku.id, originType = OrderLineItem.SkuItem, state = state)
    })
  } yield {}

  def orderNotes: Seq[Note] = {
    def newNote(body: String) = Note(referenceId = 1, referenceType = Note.Order, storeAdminId = 1, body = body)
    (1 to Random.nextInt(4)) map { i ⇒  newNote(Lorem.sentence(Random.nextInt(5))) } 
  }

  private def total(products: Seq[SimpleProductData])(implicit db: Database)  = for { 
    prices ← * <~ DbResultT.sequence(products.map(Mvp.getPrice))
    t ← * <~ prices.foldLeft(0)(_+_)
  } yield t

  private def getCc(customerId: Customer#Id)(implicit db: Database) =
    CreditCards.findDefaultByCustomerId(customerId).one
      .mustFindOr(CustomerHasNoCreditCard(customerId))

  private def getDefaultAddress(customerId: Customer#Id)(implicit db: Database) =
    Addresses.findAllByCustomerId(customerId).filter(_.isDefaultShipping).one
      .mustFindOr(CustomerHasNoDefaultAddress(customerId))
}
