package seeds.scenarios

import scala.concurrent.duration._
import scala.util.Random

import io.gatling.core.Predef._
import seeds.Addresses._
import seeds.Auth._
import seeds.Cart._
import seeds.Customers._
import seeds._

class PacificNorthwestVIPs extends Simulation {

  setUp(scenario("Pacific Northwest VIPs")
    .exec(loginAsRandomAdmin).exitHereIfFailed
    .exec(createRandomCustomers).exitHereIfFailed
    .exec(randomAddressLine1("customerAddress")).exitHereIfFailed
    .feed(csv("data/scenarios/pacific_northwest_vips/regions_cities.csv").random).exitHereIfFailed
    .exec(addCustomerAddress).exitHereIfFailed
    .exec(setDefaultShipping).exitHereIfFailed
    .repeat(_ ⇒ Random.nextInt(10) + 5)(placeOrder.exec(ageOrder).exitHereIfFailed)
    .inject(rampUsers(10) over 1.minute))
    .protocols(Conf.httpConf)
    .assertions(Conf.defaultAssertion)
}
