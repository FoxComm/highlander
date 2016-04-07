package seeds.scenarios

import scala.concurrent.duration._
import scala.util.Random

import faker._
import io.gatling.core.Predef._
import seeds._

object PacificNorthwestVIPs {

  // Regions:
  // 4177 Washington
  // 4129 California
  // 4164 Oregon

  val pacificNorthwestVIPs = scenario("Pacific Northwest VIPs")
    // Login as one of admins
    .feed(csv("data/store_admins.csv").random)
    .exec(Auth.loginAsAdmin)
    // Create a new customer
    .feed(Customers.randomCustomerFeeder)
    .exec(Customers.createCustomer)
    // Fill address
    .feed(csv("data/scenarios/pacific_northwest_vips/regions_cities.csv").random)
    .feed(csv("data/address_gen.csv").random, 3)
    .exec { session ⇒
      // Generate moderately silly street name
      val streetName = s"${session("foo1").as[String]} ${session("bar2").as[String]} ${session("baz3").as[String]}"
      session.setAll(
        ("customerAddress", s"${Random.nextInt(1234)} $streetName"),
        ("customerZip", Lorem.numerify("#####"))
      )
    }
    .exec(Addresses.addCustomerAddress)
    .exec(Addresses.setDefaultShipping)
    .inject(rampUsers(100) over 1.minute)
    // TODO place orders
}
