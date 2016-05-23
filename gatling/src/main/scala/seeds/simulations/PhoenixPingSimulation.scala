package seeds.simulations

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import seeds.Conf._
import seeds.requests.Ping._

class PhoenixPingSimulation extends Simulation {
  setUp(scenario("Ping phoenix").exec(waitForPhoenix).inject(atOnceUsers(1)))
    .assertions(global.failedRequests.percent.lessThan(99))
    .protocols(httpConf)
    .maxDuration(phoenixStartupTimeout)
}
