package seeds

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import seeds.scenarios.PacificNorthwestVIPs

class GatlingSeeds extends Simulation {

  val httpConf = http
    .baseURL("http://localhost:9090")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .disableWarmUp

  val chain = List(
    // Customers.createStaticCustomers, FIXME: add addresses for static customers
    PacificNorthwestVIPs.pacificNorthwestVIPs
  )

  setUp(chain).protocols(httpConf)

}
