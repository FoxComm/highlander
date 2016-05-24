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

  val randomCustomerActivity = scenario("Random customer activity")
    .exec(loginAsRandomAdmin)
    .stopOnFailure
    .doPause
    .exec(createRandomCustomers)
    .stopOnFailure
    .doPause
    .exec(randomAddressLine1("customerAddress"))
    .stopOnFailure
    .doPause
    .feed(
        dbFeeder("""select id as "customerRegionId", name as "customerCity" from regions""").random)
    .stopOnFailure
    .doPause
    .randomSwitch(50.0 → randomAddressLine2("customerAddress2"))
    .stopOnFailure
    .doPause
    .exec(addCustomerAddress)
    .stopOnFailure
    .doPause
    .exec(setDefaultShipping)
    .stopOnFailure
    .doPause
    .repeat(_ ⇒ nextInt(3))(placeOrder.exec(ageOrder))
    .stopOnFailure
    .doPause
    .inject(atOnceUsers(2))

  val pacificNwVips = scenario("Pacific Northwest VIPs")
    .exec(loginAsRandomAdmin)
    .stopOnFailure
    .doPause
    .exec(createRandomCustomers)
    .stopOnFailure
    .doPause
    .exec(randomAddressLine1("customerAddress"))
    .stopOnFailure
    .doPause
    .feed(csv("data/scenarios/pacific_northwest_vips/regions_cities.csv").random)
    .stopOnFailure
    .doPause
    .randomSwitch(50.0 → randomAddressLine2("customerAddress2"))
    .stopOnFailure
    .doPause
    .exec(addCustomerAddress)
    .stopOnFailure
    .doPause
    .exec(setDefaultShipping)
    .stopOnFailure
    .doPause
    .repeat(_ ⇒ nextInt(10) + 5)(placeOrder.exec(ageOrder))
    .stopOnFailure
    .doPause
    .inject(atOnceUsers(1))
}
