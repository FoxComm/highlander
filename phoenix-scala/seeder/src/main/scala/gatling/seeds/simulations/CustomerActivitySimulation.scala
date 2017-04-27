package gatling.seeds.simulations

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import gatling.seeds._
import gatling.seeds.requests.Addresses._
import gatling.seeds.requests.Auth._
import gatling.seeds.requests.Cart._
import gatling.seeds.requests.Customers._

class CustomerActivitySimulation extends Simulation {

  import CustomerActivityScenarios._

  setUp(pacificNwVips, randomCustomerActivity)
    .assertions(Conf.defaultAssertion)
    .protocols(Conf.httpConf)
}

object CustomerActivityScenarios {

  val randomCustomerActivity = scenario("Random customer activity")
    .step(loginAsRandomAdmin)
    .step(createRandomCustomer)
    .step(addRandomAddress)
    .step(placeOrder)
    .step(ageOrder)
    .inject(atOnceUsers(1))

  val pacificNwVips = scenario("Pacific Northwest VIPs")
    .step(loginAsRandomAdmin)
    .step(createRandomCustomer)
    .step(addRandomAddress)
    .step(placeOrder)
    .step(ageOrder)
    .inject(atOnceUsers(1))
}
