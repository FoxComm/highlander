package com.foxcommerce.perf.endpoints.write.storefront

import com.foxcommerce.common._
import com.foxcommerce.fixtures._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder

object AddressEndpoint {

  def create(address: AddressFixture): HttpRequestBuilder = http("Create My Address")
    .post("/api/v1/my/addresses")
    .body(StringBody(Utils.addressPayloadBody(address)))
    .check(status.is(200))
    .check(jsonPath("$.id").ofType[Long].saveAs("customerAddressId"))

  def update(address: AddressFixture): HttpRequestBuilder = http("Update My Address")
    .patch("/api/v1/my/addresses/${customerAddressId}")
    .body(StringBody(Utils.addressPayloadBody(address)))
    .check(status.is(200))

  def setAsDefault(address: AddressFixture): HttpRequestBuilder = http("Set My Address As Default")
    .post("/api/v1/my/addresses/${customerAddressId}/default")
    .check(status.is(200))

  def get(address: AddressFixture): HttpRequestBuilder = http("Get My Address")
    .get("/api/v1/my/addresses/${customerAddressId}")
    .check(status.is(200))

  def removeDefault(): HttpRequestBuilder = http("Reset My Default Address")
    .delete("/api/v1/my/addresses/default")
    .check(status.is(204))

  def delete(): HttpRequestBuilder = http("Delete My Address")
    .delete("/api/v1/my/addresses/${customerAddressId}")
    .check(status.is(204))
}
