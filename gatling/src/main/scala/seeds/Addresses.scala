package seeds

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import org.json4s.jackson.Serialization.{write ⇒ json}
import payloads.CreateAddressPayload

object Addresses {

  val addCustomerAddress = http("Add new address for customer")
    .post("/v1/customers/${customerId}/addresses")
    .header("Authorization", "${jwtTokenAdmin}")
    .body(StringBody(session ⇒ for {
      name ← session("customerName").validate[String]
      regionId ← session("customerRegionId").validate[String]
      address ← session("customerAddress").validate[String]
      city ← session("customerCity").validate[String]
      zip ← session("customerZip").validate[String]
    } yield json(CreateAddressPayload(name = name, regionId = regionId.toInt, address1 = address, city = city,
      zip = zip, isDefault = true))))
    .check(status.is(200))
    .check(jsonPath("$..id").ofType[Int].saveAs("addressId"))

  val setDefaultShipping = http("Set address as default shipping address")
    .post("/v1/customers/${customerId}/addresses/${addressId}/default")
    .check(status.is(200))
}
