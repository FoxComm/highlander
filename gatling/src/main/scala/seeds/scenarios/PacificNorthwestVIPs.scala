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
    .exec(loginAsRandomAdmin)
    .exec(createRandomCustomers)
    .exec(randomAddressLine1("customerAddress"))
    .feed(csv("data/scenarios/pacific_northwest_vips/regions_cities.csv").random)
    .exec(addCustomerAddress)
    .exec(setDefaultShipping)
    .repeat(_ ⇒ Random.nextInt(10) + 5)(placeOrder.exec(ageOrder))
    .inject(rampUsers(100) over 1.minute))
    .protocols(Conf.httpConf)
}
