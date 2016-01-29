package com.foxcommerce

import com.foxcommerce.common.{Config, Utils}
import com.foxcommerce.endpoints.{CustomerEndpoint, CustomerAddressEndpoint}
import com.foxcommerce.payloads.{CustomerPayload, CustomerAddressPayload}
import io.gatling.core.Predef._

class TrailSimulation extends Simulation {

  val conf = Config.load()

  before {
    conf.before()
  }

  // Prepare payloads
  val address = CustomerAddressPayload(name = Utils.randomString(10), regionId = 1, address1 = "Donkey street, 38",
    address2 = "Donkey street, 39", city = "Donkeyville", zip = "55555")

  val customer = CustomerPayload(name = "Max Power", email = Utils.randomEmail("maxpower"), address = Some(address))

  val customerUpdated = customer.copy(name = "Adil Wali", isBlacklisted = true, isDisabled = true)

  // Prepare scenario
  val trailScenario = scenario("Trail Scenario")
    .feed(jsonFile("data/users.json").random)
    // Create customer + address
    .exec(CustomerEndpoint.create(customer))
    .exec(CustomerAddressEndpoint.create(address))
    .pause(conf.greenRiverPause)
    .exec(CustomerEndpoint.assert(conf, customer))
    .exec(CustomerAddressEndpoint.assert(conf, address))
    // Update customer + address
    .exec(CustomerEndpoint.update(customerUpdated))
    .exec(CustomerEndpoint.blacklist(customerUpdated))
    .exec(CustomerEndpoint.disable(customerUpdated))
    .pause(conf.greenRiverPause)
    .exec(CustomerEndpoint.assert(conf, customerUpdated))

  setUp(
    trailScenario.inject(conf.defaultInjectionProfile).protocols(conf.httpConf)
  ).assertions(conf.defaultAssertion)
}
