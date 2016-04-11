package seeds

import cats.implicits._
import faker._
import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef._
import org.json4s.jackson.Serialization.{write ⇒ json}
import payloads.CreateCustomerPayload
import seeds.Auth._

object Customers {

  val createCustomer = http("Create customer")
    .post("/v1/customers")
    .requireAdminAuth
    .body(StringBody(json(
      CreateCustomerPayload(
        name = "${customerName}".some,
        email = "${customerEmail}",
        password = "${customerPassword}".some))))
    .check(status.is(200))
    .check(jsonPath("$..id").ofType[Int].saveAs("customerId"))

  implicit class CustomerCreator(val builder: ScenarioBuilder) extends AnyVal {
    def createStaticCustomers = builder
      .foreach(csv("data/customers.csv").records, "customerRecord") {
        exec(flattenMapIntoAttributes("${customerRecord}"))
          .exec(Auth.loginAsAdmin)
          .exec(createCustomer)
      }

    def createRandomCustomers = builder
      .feed(randomCustomerFeeder)
      .loginAsRandomAdmin
      .exec(createCustomer)
  }

  private val randomCustomerFeeder = Iterator.continually {
    val name = Name.name
    Map("customerName" → name, "customerEmail" → Internet.free_email(name), "customerPassword" → Lorem.words(2).head)
  }

}
