package gatling.seeds.requests

import scala.util.Random

import faker.Lorem.numerify
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import org.json4s.jackson.Serialization.{write ⇒ json}
import payloads.AddressPayloads.CreateAddressPayload
import gatling.seeds._
import gatling.seeds.requests.Auth._
import gatling.seeds.simulations._

object Addresses {

  val USA_COUNTRY_CODE = 234

  private val addressFeeder = dbFeeder(
      s"""select id as "customerRegionId", name as "customerCity" from regions where country_id=$USA_COUNTRY_CODE""")

  private val addCustomerAddress = http("Add new address for customer")
    .post("/v1/customers/${customerId}/addresses")
    .requireAdminAuth
    .body(StringBody { session ⇒
      for {
        name     ← session("customerName").validate[String]
        regionId ← session("customerRegionId").validate[String]
        address  ← session("customerAddress").validate[String]
        city     ← session("customerCity").validate[String]
      } yield
        json {
          CreateAddressPayload(name = name,
                               regionId = regionId.toInt,
                               address1 = address,
                               city = city,
                               zip = nDigits(5),
                               isDefault = true,
                               phoneNumber = Some(nDigits(10)),
                               address2 = session("customerAddress2").asOption[String])
        }
    })
    .check(jsonPath("$.id").ofType[Int].saveAs("addressId"))

  private def nDigits(n: Int): String = numerify("#" * n)

  private val setDefaultShipping = http("Set address as default shipping address").post(
      "/v1/customers/${customerId}/addresses/${addressId}/default")

  private val randomAddressLine1 = feed(csv("data/address_gen.csv").random, 3).exec { session ⇒
    val streetName =
      s"${session("foo1").as[String]} ${session("bar2").as[String]} ${session("baz3").as[String]}"
    session.set("customerAddress", s"${Random.nextInt(1234)} $streetName")
  }

  private val randomAddressLine2 = uniformRandomSwitch(
      exec(session ⇒ session.set("customerAddress2", s"Suite ${Random.nextInt(500) + 1}")),
      exec(session ⇒ session.set("customerAddress2", s"Apt ${Random.nextInt(50) + 1}"))
  )

  val addRandomAddress = step(randomAddressLine1)
    .feed(addressFeeder.random)
    .step(randomSwitch(50.0 → randomAddressLine2))
    .step(addCustomerAddress)
    .step(setDefaultShipping)
}
