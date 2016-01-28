package com.foxcommerce

import com.foxcommerce.common.{Config, Customer, Utils}
import io.gatling.core.Predef._

class CustomerSimulation extends Simulation {

  val conf = Config.load()

  before {
    conf.before()
  }

  val customerScenario = scenario("Customer Scenario")
    .feed(jsonFile("data/users.json").random)
    .exec(Customer.create(name = "John Smith", email = Utils.randomEmail("john")))
    .exec(Customer.update(name = "Max Power"))
    .exec(Customer.blacklist(isBlacklisted = true))
    .exec(Customer.disable(isDisabled = true))
    .pause(conf.greenRiverPause)
    .exec(Customer.assert(conf, name = "Max Power", isBlacklisted = true, isDisabled = true))
    .exec(Customer.update(name = "Adil Wali"))
    .exec(Customer.blacklist(isBlacklisted = false))
    .exec(Customer.disable(isDisabled = false))
    .pause(conf.greenRiverPause)
    .exec(Customer.assert(conf, name = "Adil Wali", isBlacklisted = false, isDisabled = false))

  setUp(
    customerScenario.inject(conf.defaultInjectionProfile).protocols(conf.httpConf)
  ).assertions(conf.defaultAssertion)
}