package com.foxcommerce.endpoints.admin

import com.foxcommerce.common._
import com.foxcommerce.fixtures._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder

object CustomerAddressEndpoint {

  def create(address: AddressFixture): HttpRequestBuilder = http("Create Customer Address")
    .post("/v1/customers/${customerId}/addresses")
    .body(StringBody(Utils.addressPayloadBody(address)))
    .check(status.is(200))
    .check(jsonPath("$.id").ofType[Long].saveAs("customerAddressId"))
    .check(jsonPath("$.name").ofType[String].is(address.name))
    .check(jsonPath("$.region.id").ofType[Long].is(address.regionId))
    .check(jsonPath("$.address1").ofType[String].is(address.address1))
    .check(jsonPath("$.address2").ofType[String].is(address.address2))
    .check(jsonPath("$.city").ofType[String].is(address.city))
    .check(jsonPath("$.zip").ofType[String].is(address.zip))

  def update(address: AddressFixture): HttpRequestBuilder = http("Update Customer Address")
    .patch("/v1/customers/${customerId}/addresses/${customerAddressId}")
    .body(StringBody(Utils.addressPayloadBody(address)))
    .check(status.is(200))
    .check(jsonPath("$.id").ofType[Long].saveAs("customerAddressId"))
    .check(jsonPath("$.name").ofType[String].is(address.name))
    .check(jsonPath("$.region.id").ofType[Long].is(address.regionId))
    .check(jsonPath("$.address1").ofType[String].is(address.address1))
    .check(jsonPath("$.address2").ofType[String].is(address.address2))
    .check(jsonPath("$.city").ofType[String].is(address.city))
    .check(jsonPath("$.zip").ofType[String].is(address.zip))
    .check(jsonPath("$.zip").ofType[String].is(address.zip))
}
