package seeds

import faker._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import org.json4s.jackson.Serialization.{write ⇒ json}
import payloads.CreateCustomerPayload
import seeds.Auth._

object Customers {

  val createCustomer = http("Create customer")
    .post("/v1/customers")
    .requireAdminAuth
    .body(StringBody(json(CreateCustomerPayload(
        name = Option("${customerName}"),
        email = "${customerEmail}",
        password = Option("${customerPassword}")))))
    .check(status.is(200), jsonPath("$.id").ofType[Int].saveAs("customerId"))

  val createStaticCustomers = foreach(csv("data/customers.csv").records, "customerRecord") {
    exec(flattenMapIntoAttributes("${customerRecord}")).exec(createCustomer)
  }

  private val randomCustomerFeeder = Iterator.continually {
    val name = Name.name
    Map("customerName" → name, "customerEmail" → Internet.free_email(name), "customerPassword" → Lorem.words(2).head)
  }

  val createRandomCustomers = feed(randomCustomerFeeder).exec(createCustomer)

}
