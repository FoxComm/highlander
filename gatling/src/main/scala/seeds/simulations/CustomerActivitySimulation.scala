package seeds.simulations

import scala.util.Random._

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import seeds._
import seeds.requests.Addresses._
import seeds.requests.Auth._
import seeds.requests.Cart._
import seeds.requests.Customers._

class CustomerActivitySimulation extends Simulation {

  import CustomerActivityScenarios._

  setUp(pacificNwVips, randomCustomerActivity)
    .assertions(Conf.defaultAssertion)
    .protocols(Conf.httpConf)
}

object CustomerActivityScenarios {

  private val addressFeeder = dbFeeder(
      """select id as "customerRegionId", name as "customerCity" from regions""")

  val randomCustomerActivity = scenario("Random customer activity")
    .step(loginAsRandomAdmin)
    .step(createRandomCustomers)
    .step(randomAddressLine1("customerAddress"))
    .feed(addressFeeder.random)
    .randomSwitch(50.0 → randomAddressLine2("customerAddress2"))
    .step(addCustomerAddress)
    .step(setDefaultShipping)
    .repeat(_ ⇒ nextInt(3))(placeOrder.step(ageOrder))
    .inject(atOnceUsers(2))

  val pacificNwVips = scenario("Pacific Northwest VIPs")
    .step(loginAsRandomAdmin)
    .step(createRandomCustomers)
    .step(randomAddressLine1("customerAddress"))
    .feed(csv("data/scenarios/pacific_northwest_vips/regions_cities.csv").random)
    .randomSwitch(50.0 → randomAddressLine2("customerAddress2"))
    .step(addCustomerAddress)
    .step(setDefaultShipping)
    .repeat(_ ⇒ nextInt(10) + 5)(placeOrder.step(ageOrder))
    .inject(atOnceUsers(1))
}
