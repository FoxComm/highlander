package seeds

import scala.util.Random

import faker.Lorem
import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef._
import org.json4s.jackson.Serialization.{write ⇒ json}
import payloads.CreateAddressPayload
import Auth._

object Addresses {

  val addCustomerAddress = http("Add new address for customer")
    .post("/v1/customers/${customerId}/addresses")
    .requireAdminAuth
    .body(StringBody(session ⇒ for {
      name ← session("customerName").validate[String]
      regionId ← session("customerRegionId").validate[String]
      address ← session("customerAddress").validate[String]
      city ← session("customerCity").validate[String]
    } yield json(CreateAddressPayload(name = name, regionId = regionId.toInt, address1 = address, city = city,
      zip = Lorem.numerify("#####"), isDefault = true))))
    .check(status.is(200))
    .check(jsonPath("$..id").ofType[Int].saveAs("addressId"))

  val setDefaultShipping = http("Set address as default shipping address")
    .post("/v1/customers/${customerId}/addresses/${addressId}/default")
    .check(status.is(200))

  def randomAddressLine1(attributeName: String) =
    feed(csv("data/address_gen.csv").random, 3)
      .exec { session ⇒
        val streetName = s"${session("foo1").as[String]} ${session("bar2").as[String]} ${session("baz3").as[String]}"
        session.set(attributeName, s"${Random.nextInt(1234)} $streetName")
      }

}
