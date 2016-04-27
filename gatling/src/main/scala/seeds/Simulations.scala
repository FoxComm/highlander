package seeds

import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import seeds.Scenarios._

object Simulations {

  class PhoenixPing extends Simulation {
    setUp(scenario("Ping phoenix")
    .exec(Ping.waitForPhoenix)
    .inject(atOnceUsers(1)))
    .assertions(global.failedRequests.percent.lessThan(99))
    .protocols(Conf.httpConf)
    .maxDuration(Conf.phoenixStartupTimeout)
  }

  class GatlingSeeds extends Simulation {
    setUp(pacificNwVips, randomCustomerActivity)
    .assertions(Conf.defaultAssertion)
    .protocols(Conf.httpConf)
    .maxDuration(10.minutes)
  }

}
