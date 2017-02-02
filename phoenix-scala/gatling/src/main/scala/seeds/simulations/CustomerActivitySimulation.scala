package seeds.simulations

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import seeds._
import seeds.requests.Addresses._
import seeds.requests.Auth._
import seeds.requests.Cart._
import seeds.requests.Customers._
import helpers._

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
