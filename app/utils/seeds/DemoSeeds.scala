
package utils.seeds


import models.{Customer, Customers, OrderShippingMethods, OrderShippingMethod, 
  ShippingMethod, ShippingMethods, Skus, Sku, Shipments, OrderLineItemSkus, 
  OrderLineItems, OrderLineItem, GiftCards, GiftCard, Order, Orders, CreditCards, 
  Address, Addresses, OrderPayments, OrderPayment, OrderShippingAddresses, 
  OrderShippingAddress, Shipment, GiftCardOrder, GiftCardOrders}

import models.inventory.{Warehouse, Warehouses, InventorySummary}
import models.Order.Shipped

import services.{CustomerHasNoCreditCard, CustomerHasNoDefaultAddress, NotFoundFailure404}
import services.orders.OrderTotaler
import services.Result

import utils.seeds.generators.GeneratorUtils.randomString
import utils.Money.Currency
import utils.DbResultT
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._

import scala.concurrent.ExecutionContext.Implicits.global
import slick.driver.PostgresDriver.api._
import faker._;
import cats.implicits._
import utils.time

/**
 * The scenarios below are outlined here. 
 * https://docs.google.com/document/d/1NW9v81xtMFXkvGVg8_4uzhmRVZzG2CiL8w4zefGCeV4/edit#
 */

trait DemoSeedHelpers { 

  def warehouse: Warehouse = Warehouse.buildDefault()
  def warehouses: Seq[Warehouse] = Seq(warehouse)

  def generateCustomer(name:String, email: String): Customer = 
    Customer(email = email, password = randomString(10).some, name = name.some, 
      location ="Seattle,WA".some)

  def createShippedOrder(customerId: Customer#Id, skuIds: Seq[Sku#Id], 
    shipMethod: ShippingMethod#Id)(implicit db: Database): DbResultT[Order] = for {
    order ← * <~ Orders.create(Order(state = Shipped,
      customerId = customerId, placedAt = time.yesterday.toInstant.some))
    _     ← * <~ addSkusToOrder(skuIds, order.id, OrderLineItem.Shipped)
    cc    ← * <~ getCc(customerId) // TODO: auth
    op    ← * <~ OrderPayments.create(OrderPayment.build(cc).copy(orderId = order.id, amount = none))
    addr  ← * <~ getDefaultAddress(customerId)
    shipA ← * <~ OrderShippingAddresses.create(OrderShippingAddress.buildFromAddress(addr).copy(orderId = order.id))
    shipM ← * <~ OrderShippingMethods.create(OrderShippingMethod(orderId = order.id, shippingMethodId = shipMethod))
    _     ← * <~ OrderTotaler.saveTotals(order)
    _     ← * <~ Shipments.create(Shipment(orderId = order.id, orderShippingMethodId = shipM.id.some,
                      shippingAddressId = shipA.id.some))
  } yield order

  private def addSkusToOrder(skuIds: Seq[Sku#Id], orderId: Order#Id, state: OrderLineItem.State): DbResultT[Unit] = 
    for {
      liSkus ← * <~ OrderLineItemSkus.filter(_.skuId.inSet(skuIds)).result
      _ ← * <~ OrderLineItems.createAll(liSkus.seq.map { liSku ⇒
        OrderLineItem(orderId = orderId, originId = liSku.id, originType = OrderLineItem.SkuItem, state = state)
      })
    } yield {}

  private def getCc(customerId: Customer#Id)(implicit db: Database) =
    CreditCards.findDefaultByCustomerId(customerId).one
      .mustFindOr(CustomerHasNoCreditCard(customerId))

  private def getDefaultAddress(customerId: Customer#Id)(implicit db: Database) =
    Addresses.findAllByCustomerId(customerId).filter(_.isDefaultShipping).one
      .mustFindOr(CustomerHasNoDefaultAddress(customerId))

  def createAddresses(customers: Seq[Customer#Id], address: Address): DbResultT[Seq[Int]] = for {
    addressIds ← * <~ Addresses.createAllReturningIds(customers.map{ id ⇒ address.copy(customerId = id)})
  } yield addressIds

}

/**
 * This demo seed is for the following Scenario. There are 5 Customers + 5 shoes
 * for 5 different users to test.
 *
 *  A customer calls in explaining they are just dying to get a particular pair 
 *  of shoes, but they’ve had an issue trying to add this pair of shoes to their 
 *  shopping cart.
 *
 *  Once you’ve successfully added the pair of shoes to the customer’s cart, 
 *  the customer would like you to just go ahead and place their order. Add the 
 *  customer’s information to the cart and place their order!
 */
trait DemoScenario2 extends DemoSeedHelpers {

  def customers2 = Seq(
    generateCustomer("John Vera", "john.vera@gmail.com"),
    generateCustomer("Mary Pavlik", "mary5123@comcast.net"),
    generateCustomer("Richard Bolff", "bolffcasper@gmail.com"),
    generateCustomer("Larry Cage", "cage@compuglobal.com"),
    generateCustomer("Susan Dole", "susan.dole@yahoo.com"))


  def skus2: Seq[Sku] = Seq(
    Sku(sku = "SKU-ALG", name = "Alegria Women's Vanessa Sandal".some, price = 3500),
    Sku(sku = "SKU-NIK", name = "Nike Men's Donwshifter 6 Running Shoe".some, price = 2500),
    Sku(sku = "SKU-BAL", name = "New Balance Men's M520V2 Running Shoe".some, price = 2800),
    Sku(sku = "SKU-CLK", name = "Clarks Women's Aria Pump Flat".some, price = 7900),
    Sku(sku = "SKU-ADS", name = "adidas Performance Women's Galactic Elite Running Shoe".some, price = 4900))

  def inventorySummaries(skus: Seq[Sku]): Seq[InventorySummary] = 
    skus.map { sku ⇒ InventorySummary.buildNew(warehouse.id, skuId = sku.id, onHand = 100) } 

  def address2 = Address(customerId = 0, regionId = 4177, name = "Home", 
    address1 = "555 E Lake Union St.", address2 = None, city = "Seattle", 
    zip = "12345", isDefaultShipping = true, phoneNumber = "2025550113".some)

  def createScenario2(implicit db: Database) = for { 
    warehouseIds ← * <~ Warehouses.createAllReturningIds(warehouses)
    customerIds ← * <~ Customers.createAllReturningIds(customers2)
    addressIds ← * <~ createAddresses(customerIds, address2)
    skuIds ← * <~ Skus.createAllReturningIds(skus2)
  } yield {}

}

/**
 *
 * This demo seed is for the following Scenario. There are 5 Customers + 5 shoes
 * for 5 different users to test.
 *
 * You’re still a Customer Service Employee at Tappos (you’re just loving 
 * helping all these people!). 
 *
 * This time a customer calls in complaining that they paid for 2-day shipping, 
 * but their order took 4 days to arrive. Tappos prides itself in its fast 
 * shipping and never wants their customers to have a bad experience! If an 
 * order ever arrives late, Tappos always refunds their customers the cost of 
 * shipping by issuing them Store Credit. 
 */

trait DemoScenario3 extends DemoSeedHelpers {

  def customers3 = Seq(
    generateCustomer("Mary Vera", "mary.vera@gmail.com"),
    generateCustomer("Richard Pavlik", "richard5123@comcast.net"),
    generateCustomer("Larry Bolff", "casperbolff@gmail.com"),
    generateCustomer("Susan Cage", "susan@compuglobal.com"),
    generateCustomer("John Dole", "john.dole@yahoo.com"))

  def skus3: Seq[Sku] = Seq(Sku(sku = "SKU-CLK2", name = "Clarks Women's Aria Pump Flat".some, price = 7900))

  def address3 = Address(customerId = 0, regionId = 4177, name = "Home", 
    address1 = "555 E Lake Union St.", address2 = None, city = "Seattle", 
    zip = "12345", isDefaultShipping = true, phoneNumber = "2025550113".some)

  def createScenario3(implicit db: Database) = for { 
    shippingMethod  ← * <~ ShippingMethods.filter(_.adminDisplayName === "UPS 2-day").one.mustFindOr(
      NotFoundFailure404("Unable to find 2-day shipping method"))
    warehouseIds ← * <~ Warehouses.createAllReturningIds(warehouses)
    customerIds ← * <~ Customers.createAllReturningIds(customers3)
    addressIds ← * <~ createAddresses(customerIds, address3)
    skuIds ← * <~ Skus.createAllReturningIds(skus3)
    orders ← * <~ customerIds.map { id ⇒ createShippedOrder(id, skuIds, shippingMethod.id)}
  } yield {}
}


/**
 * This demo seed is for the following Scenario. There are 5 Customers + 5 shoes
 * for 5 different users to test.
 *
 * Image you are a Customer Service employee at a large online shoe 
 * retailer - Tappos.  Your boss needs help with some end-of-year accounting. 
 * She needs to know how many $50 gift cards Tappos issued in the month of December.  
 *
 */

trait DemoScenario6 extends DemoSeedHelpers {

  def orderReferenceNum = {
    val base = new Base{}
    base.bothify("????####-##")
  }

  def customer6 = generateCustomer("Joe Carson", "carson19@yahoo.com")

  def createScenario6(implicit db: Database): DbResultT[Unit] = for {
    customer ← * <~ Customers.create(customer6)
    order ← * <~ Orders.create(Order(state = Shipped, customerId = customer.id, referenceNumber = orderReferenceNum))
    orig  ← * <~ GiftCardOrders.create(GiftCardOrder(orderId = order.id))
    _  ← * <~ GiftCards.createAll(
      (1 to 23).map { _ ⇒ 
        GiftCard.buildLineItem(balance = 50000, originId = orig.id, 
          currency = Currency.USD)
      })
  } yield {}
}

object DemoSeeds extends DemoScenario2 with DemoScenario3 with DemoScenario6 {

  def insertDemoSeeds(implicit db: Database): DbResultT[Unit] = for {
    _ ← * <~ createScenario2
    _ ← * <~ createScenario3
    _ ← * <~ createScenario6
  } yield {}

}
