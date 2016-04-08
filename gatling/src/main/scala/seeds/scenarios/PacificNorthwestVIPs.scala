package seeds.scenarios

import scala.concurrent.duration._

import io.gatling.core.Predef._
import seeds.Addresses._
import seeds.Cart._
import seeds._
import seeds.Auth._
import seeds.Customers._

class PacificNorthwestVIPs extends Simulation {

  setUp(scenario("Pacific Northwest VIPs")
    .loginAsRandomAdmin
    .createRandomCustomers
    .randomAddressLine1("customerAddress")
    .feed(csv("data/scenarios/pacific_northwest_vips/regions_cities.csv").random)
    .exec(addCustomerAddress)
    .exec(setDefaultShipping)
    .placeOrder
    .inject(rampUsers(100) over 1.minute))
    .protocols(Conf.httpConf)
}
