package com.foxcommerce

import com.foxcommerce.common._
import com.foxcommerce.endpoints._
import com.foxcommerce.payloads._
import io.gatling.core.Predef._

class TrailSimulation extends Simulation {

  val conf = Config.load()

  before {
    conf.before()
  }

  // Prepare initial payloads
  val giftCard = GiftCardPayload(balance = 200, reasonId = 1)
  val storeCredit = StoreCreditPayload(amount = 200, reasonId = 1)
  val address = AddressPayload(name = Utils.randomString(10), regionId = 1, address1 = "Donkey street, 38",
    address2 = "Donkey street, 39", city = "Donkeyville", zip = "55555")
  val customer = CustomerPayload(name = "Max Power", email = Utils.randomEmail("maxpower"), address = address,
    storeCreditCount = 1, storeCreditTotal = storeCredit.amount)
  val shippingAddress = AddressPayload(name = Utils.randomString(10), regionId = 1, address1 = "Baker street, 38",
    address2 = "Baker street, 39", city = "Londonkey", zip = "33333")
  val order = OrderPayload(customer = customer, shippingAddress = shippingAddress)

  // Copy modified payload
  val addressUpdated = address.copy(city = "Seattle", zip = "66666")
  val shippingAddressUpdated = shippingAddress.copy(city = "London", zip = "22222")
  val customerUpdated = customer.copy(name = "Adil Wali", address = addressUpdated, isBlacklisted = true,
    isDisabled = true, storeCreditTotal = 0)
  val orderUpdated = order.copy(shippingAddress = shippingAddressUpdated)

  // Prepare scenario
  val trailScenario = scenario("Trail Scenario")
    .feed(jsonFile("data/admins.json").random)
    // Create objects
    .exec(CustomerEndpoint.create(customer))
    .exec(CustomerAddressEndpoint.create(address))
    .exec(StoreCreditEndpoint.create(storeCredit))
    .exec(GiftCardEndpoint.create(giftCard))
    .exec(OrderEndpoint.create(order))
    .exec(OrderEndpoint.addShippingAddress(shippingAddress))
    .exitHereIfFailed
    // Pause and check indexes
    .pause(conf.greenRiverPause)
    .exec(SearchEndpoint.checkCustomer(conf, customer))
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
    .exec(OrderEndpoint.updateShippingAddress(shippingAddressUpdated))
    .exec(OrderEndpoint.cancel())
    .exitHereIfFailed
    // Pause and check indexes
    .pause(conf.greenRiverPause)
    .exec(SearchEndpoint.checkCustomer(conf, customerUpdated))
    .exec(SearchEndpoint.checkStoreCredit(conf, storeCredit, state = "canceled"))
    .exec(SearchEndpoint.checkGiftCard(conf, giftCard, state = "canceled"))
    .exec(SearchEndpoint.checkOrder(conf, orderUpdated, state = "canceled"))
    .exitHereIfFailed

  setUp(
    trailScenario.inject(conf.defaultInjectionProfile).protocols(conf.httpConf)
  ).assertions(conf.defaultAssertion)
}
