package gatling.seeds.requests

import faker._
import io.circe.jackson.syntax._
import io.circe.syntax._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import payloads.CustomerPayloads.CreateCustomerPayload
import utils.json.codecs._

object Customers {

  private val createCustomer = http("Create customer")
    .post("/v1/customers")
    .requireAdminAuth
    .body(StringBody(CreateCustomerPayload(name = Option("${customerName}"),
                                                email = "${customerEmail}",
                                                password = Option("${customerPassword}")).asJson.jacksonPrint))
    .check(jsonPath("$.id").ofType[Int].saveAs("customerId"))

  val createStaticCustomers = foreach(csv("data/customers.csv").records, "customerRecord") {
    exec(flattenMapIntoAttributes("${customerRecord}")).exec(createCustomer)
  }

  private val randomCustomerFeeder = Iterator.continually {
    val name = Name.name
    Map("customerName"     → name,
        "customerEmail"    → Internet.free_email(name),
        "customerPassword" → Lorem.words(2).head)
  }

  val createRandomCustomer = feed(randomCustomerFeeder).exec(createCustomer)
}
