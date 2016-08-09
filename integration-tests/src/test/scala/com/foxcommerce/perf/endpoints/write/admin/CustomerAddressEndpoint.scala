package com.foxcommerce.perf.endpoints.write.admin

import com.foxcommerce.common._
import com.foxcommerce.fixtures._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder

object CustomerAddressEndpoint {

  def create(address: AddressFixture): HttpRequestBuilder = http("Create Customer Address")
    .post("/api/v1/customers/${customerId}/addresses")
    .body(StringBody(Utils.addressPayloadBody(address)))
    .check(status.is(200))
    .check(jsonPath("$.id").ofType[Long].saveAs("customerAddressId"))

  def update(address: AddressFixture): HttpRequestBuilder = http("Update Customer Address")
    .patch("/api/v1/customers/${customerId}/addresses/${customerAddressId}")
    .body(StringBody(Utils.addressPayloadBody(address)))
    .check(status.is(200))
    .check(jsonPath("$.id").ofType[Long].saveAs("customerAddressId"))
}
