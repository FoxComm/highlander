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
    .exec(loginAsRandomAdmin).stopOnFailure
    .exec(createRandomCustomers).stopOnFailure
    .exec(randomAddressLine1("customerAddress")).stopOnFailure
    .feed(dbFeeder("""select id as "customerRegionId", name as "customerCity" from regions""").random).stopOnFailure
    .randomSwitch(50.0 → randomAddressLine2("customerAddress2")).stopOnFailure
    .exec(addCustomerAddress).stopOnFailure
    .exec(setDefaultShipping).stopOnFailure
    .repeat(_ ⇒ nextInt(3))(placeOrder.exec(ageOrder)).stopOnFailure
    .inject(constantUsersPerSec(3).during(1.minute))

  val pacificNwVips = scenario("Pacific Northwest VIPs")
    .exec(loginAsRandomAdmin).stopOnFailure
    .exec(createRandomCustomers).stopOnFailure
    .exec(randomAddressLine1("customerAddress")).stopOnFailure
    .feed(csv("data/scenarios/pacific_northwest_vips/regions_cities.csv").random).stopOnFailure
    .randomSwitch(50.0 → randomAddressLine2("customerAddress2")).stopOnFailure
    .exec(addCustomerAddress).stopOnFailure
    .exec(setDefaultShipping).stopOnFailure
    .repeat(_ ⇒ nextInt(10) + 5)(placeOrder.exec(ageOrder)).stopOnFailure
    .inject(constantUsersPerSec(1).during(1.minute))

}
