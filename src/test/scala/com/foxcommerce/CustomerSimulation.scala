package com.foxcommerce

import com.foxcommerce.common.{Config, Customer}
import io.gatling.core.Predef._

class CustomerSimulation extends Simulation {

  val config = Config.load()

  before {
    config.before()
  }

  val usersFeeder = jsonFile("data/users.json").random

  val blacklistScenario = scenario("Customer Blacklist")
    .feed(usersFeeder)
    .exec(Customer.blacklistAdd(config))
    .pause(config.greenRiverPause)
    .exec(Customer.blacklistAssert(config, isBlacklisted = true))
    .exec(Customer.blacklistRemove(config))
    .pause(config.greenRiverPause)
    .exec(Customer.blacklistAssert(config, isBlacklisted = false))

  setUp(
    blacklistScenario.inject(atOnceUsers(config.usersCount)).protocols(config.httpConf)
  ).assertions(config.defaultAssertion)
}
