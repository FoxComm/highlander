package com.foxcommerce

import com.foxcommerce.common._
import com.foxcommerce.endpoints._
import com.foxcommerce.fixtures._
import io.gatling.core.Predef._

class TrailSimulation extends Simulation {

  val conf = Config.load()

  before {
    conf.before()
  }

  // Prepare initial fixtures
  val giftCard = GiftCardFixture(balance = 200, reasonId = 1)
  val storeCredit = StoreCreditFixture(amount = 200, reasonId = 1)
  val address = AddressFixture(name = Utils.randomString(), regionId = 1, address1 = "Donkey street, 38",
    address2 = "Donkey street, 39", city = "Donkeyville", zip = "55555")
  val customer = CustomerFixture(name = "Max Power", emailPrefix = "maxpower", address = address,
    storeCreditCount = 1, storeCreditTotal = storeCredit.amount)
  val shippingAddress = AddressFixture(name = Utils.randomString(), regionId = 1, address1 = "Baker street, 38",
    address2 = "Baker street, 39", city = "Londonkey", zip = "33333")
  val order = OrderFixture(customer = customer, shippingAddress = shippingAddress)

  // Prepare modified payloads by copying original
  val addressUpdated = address.copy(city = "Seattle", zip = "66666")
  val shippingAddressUpdated = shippingAddress.copy(city = "London", zip = "22222")
  val customerUpdated = customer.copy(name = "Adil Wali", address = addressUpdated, isBlacklisted = true,
    isDisabled = true, storeCreditTotal = 0)
  val orderUpdated = order.copy(shippingAddress = shippingAddressUpdated)

  // Prepare scenario
  val trailScenario = scenario("Trail Scenario")
    .feed(jsonFile("data/admins.json").random)
    .exec(session ⇒ {
      session
        .set("customerEmail", Utils.randomEmail(customer.emailPrefix))
    })
    // Create objects
    .exec(CustomerEndpoint.create(customer))
    .exec(CustomerAddressEndpoint.create(address))
    .exec(StoreCreditEndpoint.create(storeCredit))
    .exec(GiftCardEndpoint.create(giftCard))
    .exec(OrderEndpoint.create(order))
    .exec(OrderEndpoint.addShippingAddress(order))
    .exitHereIfFailed
    // Pause and check indexes
    .pause(conf.greenRiverPause)
    //.exec(SearchEndpoint.checkCustomer(conf, customer.copy(address = shippingAddress))) // FIXME
    .exec(SearchEndpoint.checkStoreCredit(conf, storeCredit, state = "active"))
    .exec(SearchEndpoint.checkGiftCard(conf, giftCard, state = "active"))
    .exec(SearchEndpoint.checkOrder(conf, order, state = "cart"))
    .exitHereIfFailed
    // Update objects
    .exec(CustomerEndpoint.update(customerUpdated))
    .exec(CustomerEndpoint.blacklist(customerUpdated))
    .exec(CustomerEndpoint.disable(customerUpdated))
    .exec(CustomerAddressEndpoint.update(addressUpdated))
    .exec(StoreCreditEndpoint.cancel())
    .exec(GiftCardEndpoint.cancel())
    .exec(OrderEndpoint.updateShippingAddress(orderUpdated))
    .exec(OrderEndpoint.cancel())
    .exitHereIfFailed
    // Pause and check indexes
    .pause(conf.greenRiverPause)
    //.exec(SearchEndpoint.checkCustomer(conf, customerUpdated.copy(address = shippingAddress))) // FIXME
    .exec(SearchEndpoint.checkStoreCredit(conf, storeCredit, state = "canceled"))
    .exec(SearchEndpoint.checkGiftCard(conf, giftCard, state = "canceled"))
    .exec(SearchEndpoint.checkOrder(conf, orderUpdated, state = "canceled"))
    .exitHereIfFailed

  setUp(
    trailScenario.inject(conf.defaultInjectionProfile).protocols(conf.httpConf)
  ).assertions(conf.defaultAssertion)
}
