package com.foxcommerce

import com.foxcommerce.common._
import com.foxcommerce.endpoints.AuthEndpoint
import com.foxcommerce.endpoints.admin._
import com.foxcommerce.fixtures._
import io.gatling.core.Predef._

class SearchViewSimulation extends Simulation {

  val conf = Config.load()

  before {
    conf.before()
  }

  // Prepare initial fixtures
  val baseGiftCard = GiftCardFixture(balance = 200, reasonId = 1)
  val baseStoreCredit = StoreCreditFixture(amount = 200, reasonId = 1)
  val baseAddress = AddressFixture(name = Utils.randomString(), regionId = 1, address1 = "Donkey street, 38",
    address2 = "Donkey street, 39", city = "Donkeyville", zip = "55555")
  val baseCustomer = CustomerFixture(name = "Max Power", emailPrefix = "maxpower", address = baseAddress,
    storeCreditCount = 1, storeCreditTotal = baseStoreCredit.amount)
  val baseShippingAddress = AddressFixture(name = Utils.randomString(), regionId = 1, address1 = "Baker street, 38",
    address2 = "Baker street, 39", city = "Londonkey", zip = "33333")
  val baseOrder = OrderFixture(customer = baseCustomer, shippingAddress = baseShippingAddress)

  // Prepare modified payloads by copying original
  val address = baseAddress.copy(city = "Seattle", zip = "66666")
  val shippingAddress = baseShippingAddress.copy(city = "London", zip = "22222")
  val customer = baseCustomer.copy(name = "Adil Wali", address = address, isBlacklisted = true,
    isDisabled = true, storeCreditTotal = 0)
  val order = baseOrder.copy(shippingAddress = shippingAddress)

  // Prepare scenario
  val syncScenario = scenario("Search View Sync Scenario")
    .feed(jsonFile("data/admins.json").random)
    .exec(session ⇒ {
      session
        .set("customerEmail", Utils.randomEmail(baseCustomer.emailPrefix))
    })
    // Login
    .exec(AuthEndpoint.loginAsAdmin())
    .exec(StoreAdminActivity.asStoreAdmin())
    // Create objects
    .exec(CustomerEndpoint.create(baseCustomer))
    .exec(CustomerAddressEndpoint.create(baseAddress))
    .exec(StoreCreditEndpoint.create(baseStoreCredit))
    .exec(GiftCardEndpoint.create(baseGiftCard))
    .exec(OrderEndpoint.create(order))
    .exec(OrderEndpoint.addShippingAddress(order))
    .exitHereIfFailed
    // Pause and check indexes
    .tryMax(10) {
      pause(conf.greenRiverPause)
      //.exec(SearchEndpoint.checkCustomer(conf, customer.copy(address = shippingAddress))) // FIXME
      .exec(SearchEndpoint.checkStoreCredit(conf, baseStoreCredit, state = "active"))
      .exec(SearchEndpoint.checkGiftCard(conf, baseGiftCard, state = "active"))
      .exec(SearchEndpoint.checkOrder(conf, order, state = "cart"))
    }.exitHereIfFailed
    // Update objects
    .exec(CustomerEndpoint.update(customer))
    .exec(CustomerEndpoint.blacklist(customer))
    .exec(CustomerEndpoint.disable(customer))
    .exec(CustomerAddressEndpoint.update(address))
    .exec(StoreCreditEndpoint.cancel())
    .exec(GiftCardEndpoint.cancel())
    .exec(OrderEndpoint.updateShippingAddress(order))
    //.exec(OrderEndpoint.cancel())
    .exitHereIfFailed
    // Pause and check indexes
    .tryMax(10) {
      pause(conf.greenRiverPause)
      //.exec(SearchEndpoint.checkCustomer(conf, customerUpdated.copy(address = shippingAddress))) // FIXME
      .exec(SearchEndpoint.checkStoreCredit(conf, baseStoreCredit, state = "canceled"))
      .exec(SearchEndpoint.checkGiftCard(conf, baseGiftCard, state = "canceled"))
      .exec(SearchEndpoint.checkOrder(conf, order, state = "canceled"))
    }.exitHereIfFailed

  setUp(
    syncScenario.inject(conf.defaultInjectionProfile).protocols(conf.httpConf)
  ).assertions(conf.defaultAssertion)
}
