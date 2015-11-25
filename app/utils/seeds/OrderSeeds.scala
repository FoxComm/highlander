package utils.seeds

import scala.concurrent.ExecutionContext.Implicits.global

import cats.implicits._
import models.Order._
import models._
import services.GeneralFailure
import slick.driver.PostgresDriver.api._
import utils.DbResultT.implicits._
import utils.DbResultT.{DbResultT, _}
import utils.Money.Currency
import utils.Slick.implicits._

trait OrderSeeds {

  type OrderIds = (Order#Id, Order#Id, Order#Id, Order#Id, Order#Id)

  def createOrders(customerIds: CustomerSeeds#Customers, skus: InventorySeeds#Skus,
    shipMethods: ShipmentSeeds#ShippingMethods)(implicit db: Database): DbResultT[OrderIds] = for {
    o1 ← * <~ createOrder1(customerId = customerIds._1)
    o2 ← * <~ createOrder2(customerId = customerIds._1, Seq(skus._1, skus._3))
    o3 ← * <~ createOrder3(customerId = customerIds._1, Seq(skus._2, skus._4, skus._5))
    o4 ← * <~ createOrder4(customerId = customerIds._3, Seq(skus._4, skus._6))
    o5 ← * <~ createOrder5(customerId = customerIds._2, Seq(skus._1, skus._4), shipMethods._1)
  } yield (o1.id, o2.id, o3.id, o4.id, o5.id)

  def createOrder1(customerId: Customer#Id)(implicit db: Database): DbResultT[Order] = for {
    order ← * <~ Orders.create(Order(status = ManualHold, customerId = customerId))
    orig  ← * <~ GiftCardOrders.create(GiftCardOrder(orderId = order.id))
    gc    ← * <~ GiftCards.create(GiftCard.buildLineItem(balance = 7500, originId = orig.id, currency = Currency.USD))
    liGc  ← * <~ OrderLineItemGiftCards.create(OrderLineItemGiftCard(giftCardId = gc.id, orderId = order.id))
    _     ← * <~ OrderLineItems.create(OrderLineItem.buildGiftCard(order, liGc))
    addr  ← * <~ getDefaultAddress(customerId)
    _     ← * <~ OrderShippingAddresses.create(OrderShippingAddress.buildFromAddress(addr).copy(orderId = order.id))
    _     ← * <~ Notes.createAll(orderNotes.map(_.copy(referenceId = order.id)))
  } yield order

  def createOrder2(customerId: Customer#Id, skus: Seq[Sku])(implicit db: Database): DbResultT[Order] = for {
    order  ← * <~ Orders.create(Order(status = ManualHold, customerId = customerId))
    _      ← * <~ addSkusToOrder(skus.map(_.id), order.id, OrderLineItem.Pending)
    origin ← * <~ StoreCreditManuals.create(StoreCreditManual(adminId = 1, reasonId = 1))
    totals = total(skus)
    sc     ← * <~ StoreCredits.create(StoreCredit(originId = origin.id, customerId = customerId, originalBalance = totals))
    op     ← * <~ OrderPayments.create(OrderPayment.build(sc).copy(orderId = order.id, amount = totals.some))
    _      ← * <~ StoreCredits.capture(sc, op.id.some, totals) // or auth?
    addr   ← * <~ getDefaultAddress(customerId)
    _      ← * <~ OrderShippingAddresses.create(OrderShippingAddress.buildFromAddress(addr).copy(orderId = order.id))
  } yield order

  def createOrder3(customerId: Customer#Id, skus: Seq[Sku])(implicit db: Database): DbResultT[Order] = {
    import models.GiftCard.{buildAppeasement ⇒ build}
    import payloads.{GiftCardCreateByCsr ⇒ payload}
    for {
      order  ← * <~ Orders.create(Order(status = Cart, customerId = customerId))
      _      ← * <~ addSkusToOrder(skus.map(_.id), orderId = order.id, OrderLineItem.Cart)
      origin ← * <~ GiftCardManuals.create(GiftCardManual(adminId = 1, reasonId = 1))
      totals = total(skus)
      gc1    ← * <~ GiftCards.create(build(payload(balance = 2000, reasonId = 1), originId = origin.id))
      gc2    ← * <~ GiftCards.create(build(payload(balance = 3000, reasonId = 1), originId = origin.id))
      cc     ← * <~ getCc(customerId)
      _      ← * <~ OrderPayments.createAll(Seq(
        OrderPayment.build(gc1).copy(orderId = order.id, amount = 2000.some),
        OrderPayment.build(gc2).copy(orderId = order.id, amount = 3000.some),
        OrderPayment.build(cc).copy(orderId = order.id, amount = (totals - 5000).some)
      ))
      addr   ← * <~ getDefaultAddress(customerId)
      _      ← * <~ OrderShippingAddresses.create(OrderShippingAddress.buildFromAddress(addr).copy(orderId = order.id))
    } yield order
  }

  def createOrder4(customerId: Customer#Id, skus: Seq[Sku])(implicit db: Database): DbResultT[Order] = for {
    order ← * <~ Orders.create(Order(status = Cart, customerId = customerId))
    _     ← * <~ addSkusToOrder(skus.map(_.id), order.id, OrderLineItem.Cart)
    cc    ← * <~ getCc(customerId)
    _     ← * <~ OrderPayments.create(OrderPayment.build(cc).copy(orderId = order.id, amount = total(skus).some))
    addr  ← * <~ getDefaultAddress(customerId)
    _     ← * <~ OrderShippingAddresses.create(OrderShippingAddress.buildFromAddress(addr).copy(orderId = order.id))
  } yield order

  def createOrder5(customerId: Customer#Id, skus: Seq[Sku], shipMethod: OrderShippingMethod#Id)
    (implicit db: Database): DbResultT[Order] = for {
    order ← * <~ Orders.create(Order(status = Shipped, customerId = customerId))
    _     ← * <~ addSkusToOrder(skus.map(_.id), order.id, OrderLineItem.Shipped)
    cc    ← * <~ getCc(customerId) // TODO: auth
    op    ← * <~ OrderPayments.create(OrderPayment.build(cc).copy(orderId = order.id, amount = total(skus).some))
    addr  ← * <~ getDefaultAddress(customerId)
    shipA ← * <~ OrderShippingAddresses.create(OrderShippingAddress.buildFromAddress(addr).copy(orderId = order.id))
    shipM ← * <~ OrderShippingMethods.create(OrderShippingMethod(orderId = order.id, shippingMethodId = shipMethod))
    _     ← * <~ Shipments.create(Shipment(orderId = order.id, orderShippingMethodId = shipM.id.some,
                      shippingAddressId = shipA.id.some))
  } yield order

  def addSkusToOrder(skuIds: Seq[Sku#Id], orderId: Order#Id, status: OrderLineItem.Status): DbResultT[Unit] = for {
    liSkus ← * <~ OrderLineItemSkus.createAllReturningIds(skuIds.map { skuId ⇒
      OrderLineItemSku(orderId = orderId, skuId = skuId)
    })
    _ ← * <~ OrderLineItems.createAll(liSkus.seq.map { oId ⇒
      OrderLineItem(orderId = orderId, originId = oId, originType = OrderLineItem.SkuItem, status = status)
    })
  } yield {}

  def orderNotes: Seq[Note] = {
    def newNote(body: String) = Note(referenceId = 1, referenceType = Note.Order, storeAdminId = 1, body = body)
    Seq(
      newNote("This customer is a donkey."),
      newNote("No, seriously."),
      newNote("Like, an actual donkey."),
      newNote("How did a donkey even place an order on our website?")
    )
  }

  private def total(skus: Seq[Sku]) = skus.foldLeft(0)(_ + _.price)

  private def getCc(customerId: Customer#Id)(implicit db: Database) =
    CreditCards.findDefaultByCustomerId(customerId).one
      .mustFindOr(GeneralFailure(s"No cc found for customer with id=$customerId"))

  private def getDefaultAddress(customerId: Customer#Id)(implicit db: Database) =
    Addresses.findAllByCustomerId(customerId).filter(_.isDefaultShipping).one
      .mustFindOr(GeneralFailure(s"No default address found for customer with id =$customerId"))

  def order = Order(customerId = 0, referenceNumber = "ABCD1234-11", status = ManualHold)

  def cart = order.copy(status = Order.Cart)
}
