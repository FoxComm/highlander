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
