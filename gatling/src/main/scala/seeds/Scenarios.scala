package seeds

import scala.concurrent.duration._
import scala.util.Random.nextInt

import io.gatling.core.Predef._
import seeds.Addresses._
import seeds.Auth._
import seeds.Cart._
import seeds.Customers._
import seeds.GatlingApp._

object Scenarios {

  val randomCustomerActivity = scenario("Random customer activity")
    .exec(loginAsRandomAdmin).stopOnFailure.doPause
    .exec(createRandomCustomers).stopOnFailure.doPause
    .exec(randomAddressLine1("customerAddress")).stopOnFailure.doPause
    .feed(dbFeeder("""select id as "customerRegionId", name as "customerCity" from regions""").random).stopOnFailure.doPause
    .randomSwitch(50.0 → randomAddressLine2("customerAddress2")).stopOnFailure.doPause
    .exec(addCustomerAddress).stopOnFailure.doPause
    .exec(setDefaultShipping).stopOnFailure.doPause
    .repeat(_ ⇒ nextInt(3))(placeOrder.exec(ageOrder)).stopOnFailure.doPause
    .inject(rampUsers(100) over 5.minutes)

  val pacificNwVips = scenario("Pacific Northwest VIPs")
    .exec(loginAsRandomAdmin).stopOnFailure.doPause
    .exec(createRandomCustomers).stopOnFailure.doPause
    .exec(randomAddressLine1("customerAddress")).stopOnFailure.doPause
    .feed(csv("data/scenarios/pacific_northwest_vips/regions_cities.csv").random).stopOnFailure.doPause
    .randomSwitch(50.0 → randomAddressLine2("customerAddress2")).stopOnFailure.doPause
    .exec(addCustomerAddress).stopOnFailure.doPause
    .exec(setDefaultShipping).stopOnFailure.doPause
    .repeat(_ ⇒ nextInt(10) + 5)(placeOrder.exec(ageOrder)).stopOnFailure.doPause
    .inject(rampUsers(50) over 5.minutes)

}
