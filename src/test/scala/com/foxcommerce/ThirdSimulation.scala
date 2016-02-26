package com.foxcommerce

import com.foxcommerce.common._
import com.foxcommerce.endpoints.storefront._
import com.foxcommerce.fixtures._
import io.gatling.core.Predef._

class ThirdSimulation extends Simulation {

  val conf = Config.load()

  before {
    conf.before()
  }

  val baseAddress = AddressFixture(name = Utils.randomString(), regionId = 1, address1 = "Donkey street, 38",
    address2 = "Donkey street, 39", city = "Donkeyville", zip = "55555")
  val address = baseAddress.copy(city = "Seattle", zip = "66666")    

  // Prepare scenario
  val thirdScenario = scenario("Third Scenario")
    .feed(jsonFile("data/customers.json").random)
    .exec(AccountEndpoint.get())
    .exitHereIfFailed
    // Customer Address Activity
    .exec(AddressEndpoint.create(baseAddress))
    .exec(AddressEndpoint.update(address))
    .exec(AddressEndpoint.get(address))
    .exec(AddressEndpoint.setAsDefault(address))
    .exitHereIfFailed
    // Cleanup
    .exec(AddressEndpoint.removeDefault())
    .exec(AddressEndpoint.delete())

  setUp(
    thirdScenario.inject(atOnceUsers(1)).protocols(conf.httpConf)
  ).assertions(conf.defaultAssertion)
}
