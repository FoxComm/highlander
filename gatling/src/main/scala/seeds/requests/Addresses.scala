package seeds.requests

import scala.util.Random

import faker.Lorem.numerify
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import org.json4s.jackson.Serialization.{write ⇒ json}
import payloads.AddressPayloads.CreateAddressPayload
import seeds.requests.Auth._

object Addresses {

  val addCustomerAddress = http("Add new address for customer")
    .post("/v1/customers/${customerId}/addresses")
    .requireAdminAuth
    .body(
        StringBody(
            session ⇒
              for {
        name ← session("customerName").validate[String]
        regionId ← session("customerRegionId").validate[String]
        address ← session("customerAddress").validate[String]
        city ← session("customerCity").validate[String]
      } yield
                json(CreateAddressPayload(name = name,
                                          regionId = regionId.toInt,
                                          address1 = address,
                                          city = city,
                                          zip = nDigits(5),
                                          isDefault = true,
                                          phoneNumber = Some(nDigits(10)),
                                          address2 =
                                            session("customerAddress2")
                                              .asOption[String]))))
    .check(status.is(200), jsonPath("$.id").ofType[Int].saveAs("addressId"))

  private def nDigits(n: Int): String = numerify("#" * n)

  val setDefaultShipping = http("Set address as default shipping address")
    .post("/v1/customers/${customerId}/addresses/${addressId}/default")

  def randomAddressLine1(attributeName: String) =
    feed(csv("data/address_gen.csv").random, 3).exec { session ⇒
      val streetName = s"${session("foo1").as[String]} ${session("bar2")
        .as[String]} ${session("baz3").as[String]}"
      session.set(attributeName, s"${Random.nextInt(1234)} $streetName")
    }

  def randomAddressLine2(attributeName: String) =
    uniformRandomSwitch(
        exec(session ⇒
              session.set(attributeName, s"Suite ${Random.nextInt(500) + 1}")),
        exec(session ⇒
              session.set(attributeName, s"Apt ${Random.nextInt(50) + 1}"))
    )
}
