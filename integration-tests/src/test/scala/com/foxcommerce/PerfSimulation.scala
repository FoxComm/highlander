package com.foxcommerce

import com.foxcommerce.common._
import com.foxcommerce.perf.endpoints.AuthEndpoint
import com.foxcommerce.perf.endpoints.write.storefront._
import com.foxcommerce.perf.endpoints.read.storefront._
import com.foxcommerce.fixtures._
import io.gatling.core.Predef._
import io.gatling.http.Predef._

class PerfSimulation extends Simulation {

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
  val storefrontScenario = scenario("Storefront R/W Scenario")
    .feed(jsonFile("data/customers.json").random)
    .exec(AnonCustomerEndpoint.frontpage())
    .exec(AnonCustomerEndpoint.getProductList())
    .exec(AnonCustomerEndpoint.getProductPdps())
    .exec(AuthEndpoint.loginAsCustomer())
    .exec(AccountEndpoint.get())
    .exitHereIfFailed
    .exec(CartEndpoint.getCart())
    .exec(CartEndpoint.addLineItems())

  setUp(
    storefrontScenario.inject(conf.defaultInjectionProfile).protocols(conf.httpConf)
  ).assertions(conf.defaultAssertion)
}
