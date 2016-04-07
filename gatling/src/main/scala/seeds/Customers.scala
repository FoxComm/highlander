package seeds

import scala.concurrent.duration._

import cats.implicits._
import faker._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import org.json4s.jackson.Serialization.{write ⇒ json}
import payloads.CreateCustomerPayload

object Customers {

  val createCustomer = http("Create customer")
    .post("/v1/customers")
    .header("Authorization", "${jwtTokenAdmin}")
    .body(StringBody(json(
      CreateCustomerPayload(
        name = "${customerName}".some,
        email = "${customerEmail}",
        password = "${customerPassword}".some))))
    .check(status.is(200))
    .check(jsonPath("$..id").ofType[Int].saveAs("customerId"))

  val createStaticCustomers = scenario("Create customers from CSV")
    .feed(csv("data/store_admins.csv").random)
    .foreach(csv("data/customers.csv").records, "customerRecord") {
      exec(flattenMapIntoAttributes("${customerRecord}"))
        .exec(Auth.loginAsAdmin)
        .exec(createCustomer)
    }
    .inject(atOnceUsers(1))

  val randomCustomerFeeder = Iterator.continually {
    val name = Name.name
    Map("customerName" → name, "customerEmail" → Internet.free_email(name), "customerPassword" → Lorem.words(2).head)
  }

  val createRandomCustomers = scenario("Create random customers")
    .feed(randomCustomerFeeder)
    .feed(csv("data/store_admins.csv").random)
    .exec(Auth.loginAsAdmin)
    .exec(createCustomer)
    .inject(rampUsers(100) over 1.minute)
}
