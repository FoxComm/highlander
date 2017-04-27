package gatling.seeds.simulations

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import gatling.seeds.Conf._
import gatling.seeds._
import gatling.seeds.requests.Auth._

class OneshotSeedsSimulation extends Simulation {
  setUp(
      scenario("Base data")
        .step(loginAsRandomAdmin)
        // Add seeds here
        .inject(atOnceUsers(1)))
    .assertions(global.failedRequests.percent.lessThan(99))
    .protocols(httpConf)
}
