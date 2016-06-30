package utils.seeds

import scala.concurrent.ExecutionContext.Implicits.global

import cats.implicits._
import failures.CreditCardFailures.CustomerHasNoCreditCard
import failures.CustomerFailures.CustomerHasNoDefaultAddress
import failures.ShippingMethodFailures.ShippingMethodIsNotFound
import models.customer.Customer
import models.location.Addresses
import models.objects.ObjectContext
import models.order.Order._
import models.order._
import models.order.lineitems._
import models.payment.creditcard.CreditCards
import models.payment.giftcard._
import models.payment.storecredit._
import models.product.{Mvp, SimpleProductData}
import models.shipping.{Shipment, Shipments, ShippingMethods}
import models.{Note, Notes}
import services.orders.OrderTotaler
import slick.driver.PostgresDriver.api._
import utils.Money.Currency
import utils.db.DbResultT._
import utils.db._
import utils.time

trait OrderSeeds {

  type OrderIds = (Order#Id, Order#Id, Order#Id, Order#Id, Order#Id)

  def createOrders(customerIds: CustomerSeeds#Customers,
                   products: ProductSeeds#SeedProducts,
                   shipMethods: ShipmentSeeds#ShippingMethods,
                   context: ObjectContext)(implicit db: Database): DbResultT[OrderIds] =
    for {
      o1 ← * <~ createOrder1(customerId = customerIds._1, context = context)
      o2 ← * <~ createOrder2(customerId = customerIds._1,
                             context = context,
                             Seq(products._1, products._3))
      o3 ← * <~ createOrder3(customerId = customerIds._1,
                             context = context,
                             Seq(products._2, products._4, products._5))
      o4 ← * <~ createOrder4(customerId = customerIds._3,
                             context = context,
                             Seq(products._4, products._6))
      o5 ← * <~ createOrder5(customerId = customerIds._2,
                             context = context,
                             Seq(products._1, products._4),
                             shipMethods._1)
    } yield (o1.id, o2.id, o3.id, o4.id, o5.id)

  def createOrder1(customerId: Customer#Id, context: ObjectContext)(
      implicit db: Database): DbResultT[Order] =
    for {
      order ← * <~ Orders.create(
                 Order(state = ManualHold, customerId = customerId, contextId = context.id))
      orig ← * <~ GiftCardOrders.create(GiftCardOrder(orderRef = order.refNum))
      gc ← * <~ GiftCards.create(
              GiftCard.buildLineItem(balance = 7500, originId = orig.id, currency = Currency.USD))
      liGc ← * <~ OrderLineItemGiftCards.create(
                OrderLineItemGiftCard(giftCardId = gc.id, orderRef = order.refNum))
      _    ← * <~ OrderLineItems.create(OrderLineItem.buildGiftCard(order, liGc))
      addr ← * <~ getDefaultAddress(customerId)
      _ ← * <~ OrderShippingAddresses.create(
             OrderShippingAddress.buildFromAddress(addr).copy(orderRef = order.refNum))
      _ ← * <~ Notes.createAll(orderNotes.map(_.copy(referenceId = order.id)))
      _ ← * <~ OrderTotaler.saveTotals(order)
    } yield order

  def createOrder2(customerId: Customer#Id,
                   context: ObjectContext,
                   products: Seq[SimpleProductData])(implicit db: Database): DbResultT[Order] =
    for {
      order ← * <~ Orders.create(
                 Order(state = ManualHold, customerId = customerId, contextId = context.id))
      _      ← * <~ addSkusToOrder(products.map(_.skuId), order.refNum, OrderLineItem.Pending)
      origin ← * <~ StoreCreditManuals.create(StoreCreditManual(adminId = 1, reasonId = 1))
      totals ← * <~ total(products)
      sc ← * <~ StoreCredits.create(
              StoreCredit(originId = origin.id, customerId = customerId, originalBalance = totals))
      op ← * <~ OrderPayments.create(
              OrderPayment.build(sc).copy(orderRef = order.refNum, amount = totals.some))
      _    ← * <~ StoreCredits.capture(sc, op.id.some, totals) // or auth?
      addr ← * <~ getDefaultAddress(customerId)
      _ ← * <~ OrderShippingAddresses.create(
             OrderShippingAddress.buildFromAddress(addr).copy(orderRef = order.refNum))
      _ ← * <~ OrderTotaler.saveTotals(order)
    } yield order

  def createOrder3(customerId: Customer#Id,
                   context: ObjectContext,
                   products: Seq[SimpleProductData])(implicit db: Database): DbResultT[Order] = {
    import GiftCard.{buildAppeasement ⇒ build}
    import payloads.GiftCardPayloads.{GiftCardCreateByCsr ⇒ payload}

    for {
      order ← * <~ Orders.create(
                 Order(state = Cart, customerId = customerId, contextId = context.id))
      _      ← * <~ addSkusToOrder(products.map(_.skuId), orderRef = order.refNum, OrderLineItem.Cart)
      origin ← * <~ GiftCardManuals.create(GiftCardManual(adminId = 1, reasonId = 1))
      totals ← * <~ total(products)
      gc1 ← * <~ GiftCards.create(
               build(payload(balance = 2000, reasonId = 1), originId = origin.id))
      gc2 ← * <~ GiftCards.create(
               build(payload(balance = 3000, reasonId = 1), originId = origin.id))
      cc ← * <~ getCc(customerId)
      _ ← * <~ OrderPayments.createAll(
             Seq(
                 OrderPayment.build(gc1).copy(orderRef = order.refNum, amount = 2000.some),
                 OrderPayment.build(gc2).copy(orderRef = order.refNum, amount = 3000.some),
                 OrderPayment.build(cc).copy(orderRef = order.refNum, amount = none)
             ))
      addr ← * <~ getDefaultAddress(customerId)
      _ ← * <~ OrderShippingAddresses.create(
             OrderShippingAddress.buildFromAddress(addr).copy(orderRef = order.refNum))
      _ ← * <~ OrderTotaler.saveTotals(order)
    } yield order
  }

  def createOrder4(customerId: Customer#Id,
                   context: ObjectContext,
                   products: Seq[SimpleProductData])(implicit db: Database): DbResultT[Order] =
    for {
      order ← * <~ Orders.create(
                 Order(state = Cart, customerId = customerId, contextId = context.id))
      _  ← * <~ addSkusToOrder(products.map(_.skuId), order.refNum, OrderLineItem.Cart)
      cc ← * <~ getCc(customerId)
      _ ← * <~ OrderPayments.create(
             OrderPayment.build(cc).copy(orderRef = order.refNum, amount = none))
      addr ← * <~ getDefaultAddress(customerId)
      _ ← * <~ OrderShippingAddresses.create(
             OrderShippingAddress.buildFromAddress(addr).copy(orderRef = order.refNum))
      _ ← * <~ OrderTotaler.saveTotals(order)
    } yield order

  def createOrder5(customerId: Customer#Id,
                   context: ObjectContext,
                   products: Seq[SimpleProductData],
                   shipMethodId: OrderShippingMethod#Id)(implicit db: Database): DbResultT[Order] =
    for {
      order ← * <~ Orders.create(
                 Order(state = Shipped,
                       customerId = customerId,
                       contextId = context.id,
                       placedAt = Some(time.yesterday.toInstant)))
      _  ← * <~ addSkusToOrder(products.map(_.skuId), order.refNum, OrderLineItem.Shipped)
      cc ← * <~ getCc(customerId) // TODO: auth
      op ← * <~ OrderPayments.create(
              OrderPayment.build(cc).copy(orderRef = order.refNum, amount = none))
      addr ← * <~ getDefaultAddress(customerId)
      shipA ← * <~ OrderShippingAddresses.create(
                 OrderShippingAddress.buildFromAddress(addr).copy(orderRef = order.refNum))
      meth ← * <~ getShipMethod(shipMethodId)
      shipM ← * <~ OrderShippingMethods.create(
                 OrderShippingMethod.build(order = order, method = meth))
      _ ← * <~ OrderTotaler.saveTotals(order)
      _ ← * <~ Shipments.create(
             Shipment(orderRef = order.refNum,
                      orderShippingMethodId = shipM.id.some,
                      shippingAddressId = shipA.id.some))
    } yield order

  def addSkusToOrder(skuIds: Seq[Int],
                     orderRef: String,
                     state: OrderLineItem.State): DbResultT[Unit] =
    for {
      liSkus ← * <~ OrderLineItemSkus.filter(_.skuId.inSet(skuIds)).result
      _ ← * <~ OrderLineItems.createAll(liSkus.seq.map { liSku ⇒
           OrderLineItem(orderRef = orderRef,
                         originId = liSku.id,
                         originType = OrderLineItem.SkuItem,
                         state = state)
         })
    } yield {}

  def orderNotes: Seq[Note] = {
    def newNote(body: String) =
      Note(referenceId = 1, referenceType = Note.Order, storeAdminId = 1, body = body)
    Seq(
        newNote("This customer is a donkey."),
        newNote("No, seriously."),
        newNote("Like, an actual donkey."),
        newNote("How did a donkey even place an order on our website?")
    )
  }

  private def total(products: Seq[SimpleProductData])(implicit db: Database) =
    for {
      prices ← * <~ products.map(p ⇒ Mvp.getPrice(p.skuId))
      t      ← * <~ prices.sum
    } yield t

  private def getCc(customerId: Customer#Id)(implicit db: Database) =
    CreditCards
      .findDefaultByCustomerId(customerId)
      .mustFindOneOr(CustomerHasNoCreditCard(customerId))

  private def getDefaultAddress(customerId: Customer#Id)(implicit db: Database) =
    Addresses
      .findAllByCustomerId(customerId)
      .filter(_.isDefaultShipping)
      .mustFindOneOr(CustomerHasNoDefaultAddress(customerId))

  private def getShipMethod(shipMethodId: Int)(implicit db: Database) =
    ShippingMethods
      .findActiveById(shipMethodId)
      .mustFindOneOr(ShippingMethodIsNotFound(shipMethodId))

  def order =
    Order(customerId = 0, referenceNumber = "ABCD1234-11", state = ManualHold, contextId = 1)

  def cart = order.copy(state = Order.Cart)
}
