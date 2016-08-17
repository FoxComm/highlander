package utils.seeds

import scala.concurrent.ExecutionContext.Implicits.global

import cats.implicits._
import failures.CustomerFailures.CustomerHasNoDefaultAddress
import failures.ShippingMethodFailures.ShippingMethodNotFoundByName
import models.cord.Order._
import models.cord._
import models.cord.lineitems._
import models.customer.{Customer, Customers}
import models.inventory._
import models.location.{Address, Addresses}
import models.objects.ObjectContexts
import models.payment.creditcard.CreditCards
import models.payment.giftcard._
import models.product.{Mvp, SimpleContext, SimpleProductData}
import models.shipping._
import services.carts.CartTotaler
import services.orders.OrderTotaler
import slick.driver.PostgresDriver.api._
import utils.Money.Currency
import utils.Passwords.hashPassword
import utils.aliases._
import utils.db._
import utils.seeds.generators.GeneratorUtils.randomString
import utils.seeds.generators.ProductGenerator
import utils.time

/**
  * The scenarios below are outlined here.
  * https://docs.google.com/document/d/1NW9v81xtMFXkvGVg8_4uzhmRVZzG2CiL8w4zefGCeV4/edit#
  */
trait DemoSeedHelpers extends CreditCardSeeds {

  val hashedPassword = hashPassword(randomString(10))

  def generateCustomer(name: String, email: String): Customer =
    Customer(email = email.some,
             hashedPassword = hashedPassword.some,
             name = name.some,
             location = "Seattle, WA".some)

  def createShippedOrder(customerId: Customer#Id,
                         contextId: Int,
                         skuIds: Seq[Sku#Id],
                         shipMethod: ShippingMethod)(implicit db: DB): DbResultT[Order] =
    for {
      cart ← * <~ Carts.create(Cart(customerId = customerId))
      skus ← * <~ addSkusToCart(skuIds, cart.refNum)
      _    ← * <~ CartTotaler.saveTotals(cart)
      cc   ← * <~ CreditCards.create(creditCard1.copy(customerId = customerId))
      op ← * <~ OrderPayments.create(
              OrderPayment.build(cc).copy(cordRef = cart.refNum, amount = none))
      addr ← * <~ getDefaultAddress(customerId)
      shipA ← * <~ OrderShippingAddresses.create(
                 OrderShippingAddress.buildFromAddress(addr).copy(cordRef = cart.refNum))
      shipM ← * <~ OrderShippingMethods.create(
                 OrderShippingMethod.build(cordRef = cart.refNum, method = shipMethod))
      _     ← * <~ CartTotaler.saveTotals(cart)
      order ← * <~ Orders.createFromCart(cart, contextId)
      lineItems ← * <~ OrderLineItems
                   .filter(_.cordRef === cart.referenceNumber)
                   .filter(_.referenceNumber inSet skus.map(_.referenceNumber))
                   .result
      _ ← * <~ lineItems.map(lineItem ⇒
               OrderLineItems.update(lineItem, lineItem.copy(state = OrderLineItem.Shipped)))
      order ← * <~ Orders.update(order, order.copy(state = FulfillmentStarted))
      order ← * <~ Orders.update(order,
                                 order.copy(state = Shipped, placedAt = time.yesterday.toInstant))
      _ ← * <~ Shipments.create(
             Shipment(cordRef = cart.refNum,
                      orderShippingMethodId = shipM.id.some,
                      shippingAddressId = shipA.id.some))
      _ ← * <~ OrderTotaler.saveTotals(cart, order)
    } yield order

  private def addSkusToCart(skuIds: Seq[Sku#Id],
                            cordRef: String): DbResultT[Seq[CartLineItemSku]] = {
    val itemsToInsert = skuIds.map(skuId ⇒ CartLineItemSku(cordRef = cordRef, skuId = skuId))
    CartLineItemSkus.createAllReturningModels(itemsToInsert)
  }

  private def getDefaultAddress(customerId: Customer#Id)(implicit db: DB) =
    Addresses
      .findAllByCustomerId(customerId)
      .filter(_.isDefaultShipping)
      .mustFindOneOr(CustomerHasNoDefaultAddress(customerId))

  def createAddresses(customers: Seq[Customer#Id], address: Address): DbResultT[Seq[Int]] =
    for {
      addressIds ← * <~ Addresses.createAllReturningIds(customers.map { id ⇒
                    address.copy(customerId = id)
                  })
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

  def customers2 =
    Seq(generateCustomer("John Vera", "john.vera@gmail.com"),
        generateCustomer("Mary Pavlik", "mary5123@comcast.net"),
        generateCustomer("Richard Bolff", "bolffcasper@gmail.com"),
        generateCustomer("Larry Cage", "cage@compuglobal.com"),
        generateCustomer("Susan Dole", "susan.dole@yahoo.com"))

  def products2: Seq[SimpleProductData] =
    Seq(SimpleProductData(code = "SKU-ALG",
                          title = "Alegria Women's Vanessa Sandal",
                          description = "Alegria Women's Vanessa Sandal",
                          price = 3500,
                          image = ProductGenerator.randomImage),
        SimpleProductData(code = "SKU-NIK",
                          title = "Nike Men's Donwshifter 6 Running Shoe",
                          description = "Nike Men's Donwshifter 6 Running Shoe",
                          price = 2500,
                          image = ProductGenerator.randomImage),
        SimpleProductData(code = "SKU-BAL",
                          title = "New Balance Men's M520V2 Running Shoe",
                          description = "New Balance Men's M520V2 Running Shoe",
                          price = 2800,
                          image = ProductGenerator.randomImage),
        SimpleProductData(code = "SKU-CLK",
                          title = "Clarks Women's Aria Pump Flat",
                          description = "Clarks Women's Aria Pump Flat",
                          price = 7900,
                          image = ProductGenerator.randomImage),
        SimpleProductData(code = "SKU-ADS",
                          title = "adidas Performance Women's Galactic Elite Running Shoe",
                          description = "adidas Performance Women's Galactic Elite Running Shoe",
                          price = 4900,
                          image = ProductGenerator.randomImage))

  def address2 =
    Address(customerId = 0,
            regionId = 4177,
            name = "Home",
            address1 = "555 E Lake Union St.",
            address2 = None,
            city = "Seattle",
            zip = "12345",
            isDefaultShipping = true,
            phoneNumber = "2025550113".some)

  def createScenario2(implicit db: DB) =
    for {
      context     ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
      customerIds ← * <~ Customers.createAllReturningIds(customers2)
      addressIds  ← * <~ createAddresses(customerIds, address2)
      productData ← * <~ Mvp.insertProducts(products2, context.id)
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

  def customers3 =
    Seq(generateCustomer("Mary Vera", "mary.vera@gmail.com"),
        generateCustomer("Richard Pavlik", "richard5123@comcast.net"),
        generateCustomer("Larry Bolff", "casperbolff@gmail.com"),
        generateCustomer("Susan Cage", "susan@compuglobal.com"),
        generateCustomer("John Dole", "john.dole@yahoo.com"))

  def products3: Seq[SimpleProductData] =
    Seq(
        SimpleProductData(code = "SKU-CLK2",
                          title = "Clarks Women's Aria Pump Flat",
                          description = "Clarks Women's Aria Pump Flat",
                          price = 7900,
                          image = ProductGenerator.randomImage))

  def address3 =
    Address(customerId = 0,
            regionId = 4177,
            name = "Home",
            address1 = "555 E Lake Union St.",
            address2 = None,
            city = "Seattle",
            zip = "12345",
            isDefaultShipping = true,
            phoneNumber = "2025550113".some)

  def createScenario3(implicit db: DB) =
    for {
      context ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
      shippingMethod ← * <~ ShippingMethods
                        .filter(_.adminDisplayName === ShippingMethod.expressShippingNameForAdmin)
                        .mustFindOneOr(ShippingMethodNotFoundByName(
                                ShippingMethod.expressShippingNameForAdmin))
      customerIds ← * <~ Customers.createAllReturningIds(customers3)
      addressIds  ← * <~ createAddresses(customerIds, address3)
      productData ← * <~ Mvp.insertProducts(products3, context.id)
      skuIds      ← * <~ productData.map(_.skuId)
      orders ← * <~ customerIds.map(id ⇒
                    createShippedOrder(id, context.id, skuIds, shippingMethod))
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

  def customer6 = generateCustomer("Joe Carson", "carson19@yahoo.com")

  def createScenario6(implicit db: DB): DbResultT[Unit] =
    for {
      context  ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
      customer ← * <~ Customers.create(customer6)
      cart     ← * <~ Carts.create(Cart(customerId = customer.id))
      order    ← * <~ Orders.createFromCart(cart, context.id)
      order    ← * <~ Orders.update(order, order.copy(state = FulfillmentStarted))
      order ← * <~ Orders.update(order,
                                 order.copy(state = Shipped, placedAt = time.yesterday.toInstant))
      _    ← * <~ OrderTotaler.saveTotals(cart, order)
      orig ← * <~ GiftCardOrders.create(GiftCardOrder(cordRef = cart.refNum))
      _ ← * <~ GiftCards.createAll((1 to 23).map { _ ⇒
           GiftCard.buildLineItem(balance = 50000, originId = orig.id, currency = Currency.USD)
         })
    } yield {}
}

object DemoSeeds extends DemoScenario2 with DemoScenario3 with DemoScenario6 {

  def insertDemoSeeds(implicit db: DB): DbResultT[Unit] =
    for {
      _ ← * <~ createScenario2
      _ ← * <~ createScenario3
      _ ← * <~ createScenario6
    } yield {}
}
