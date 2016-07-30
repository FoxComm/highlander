package com.foxcommerce

import com.foxcommerce.common._
import com.foxcommerce.endpoints.AuthEndpoint
import com.foxcommerce.endpoints.storefront._
import com.foxcommerce.fixtures._
import io.gatling.core.Predef._
import io.gatling.http.Predef._

class StorefrontAccessSimulation extends Simulation {

  val conf = Config.load()

  before {
    conf.before()
  }

  // Fixtures
  val baseAddress = AddressFixture(name = Utils.randomString(), regionId = 1, address1 = "Donkey street, 38",
    address2 = "Donkey street, 39", city = "Donkeyville", zip = "55555")
  val baseCreditCard = CreditCardFixture(holderName = "John Smith", number = "4242424242424242", cvv = "055",
    expMonth = 1, expYear = 2020)

  val address = baseAddress.copy(city = "Seattle", zip = "66666")
  val creditCard = baseCreditCard.copy(holderName = "Adil Wali")

  // Prepare scenario
  val storefrontScenario = scenario("Storefront Access Scenario")
    .feed(jsonFile("data/customers.json").random)
    // Customer Init and Intruder Registration
    .exec(session ⇒ {
      session
        .set("intruderEmail", Utils.randomEmail("intruder"))
        .set("intruderPassword", Utils.randomString())
    })
    // Intruder registration and login, customer login
    .exec(IntruderActivity.register())
    .exec(AuthEndpoint.loginAsIntruder())
    .exec(AuthEndpoint.loginAsCustomer())
    .exec(flushCookieJar)
    .exitHereIfFailed
    // Customer Address Activity
    .exec(CustomerActivity.asCustomer())
    .exec(AddressEndpoint.create(baseAddress))
    .exec(AddressEndpoint.update(address))
    .exec(AddressEndpoint.get(address))
    .exec(AddressEndpoint.setAsDefault(address))
    .exitHereIfFailed
    // Customer Credit Card Activity
    .exec(session ⇒ {
      session
        .set("ccHolderName", baseCreditCard.holderName)
        .set("ccNumber", baseCreditCard.number)
        .set("ccCvv", baseCreditCard.cvv)
        .set("ccExpMonth", baseCreditCard.expMonth)
        .set("ccExpYear", baseCreditCard.expYear)
    })
    .exec(CreditCardEndpoint.create(baseCreditCard, address))
    .exec(CreditCardEndpoint.update(creditCard, address))
    .exec(CreditCardEndpoint.get(creditCard, address))
    .exec(CreditCardEndpoint.setAsDefault(creditCard, address))
    .exitHereIfFailed
    // Intruder Address Activity
    .exec(IntruderActivity.asIntruder())
    .exec(IntruderActivity.Address.get())
    .exec(IntruderActivity.Address.update(address))
    .exec(IntruderActivity.Address.setAsDefault())
    .exec(IntruderActivity.Address.delete())
    .exec(IntruderActivity.Cart.shippingAddressAdd())
    // Intruder Credit Card Activity
    .exec(IntruderActivity.CreditCard.get())
    .exec(IntruderActivity.CreditCard.create())
    .exec(IntruderActivity.CreditCard.update(address))
    .exec(IntruderActivity.CreditCard.setAsDefault())
    .exec(IntruderActivity.CreditCard.delete())
    // Cleanup
    .exec(CustomerActivity.asCustomer())
    .exec(AddressEndpoint.removeDefault())
    .exec(AddressEndpoint.delete())
    .exec(CreditCardEndpoint.delete())

  setUp(
    storefrontScenario.inject(conf.defaultInjectionProfile).protocols(conf.httpConf)
  ).assertions(conf.defaultAssertion)
}
