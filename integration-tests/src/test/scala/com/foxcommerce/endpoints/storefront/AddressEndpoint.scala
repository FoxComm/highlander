package com.foxcommerce.endpoints.storefront

import com.foxcommerce.common._
import com.foxcommerce.fixtures._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder

object AddressEndpoint {

  def create(address: AddressFixture): HttpRequestBuilder = http("Create My Address")
    .post("/v1/my/addresses")
    .body(StringBody(Utils.addressPayloadBody(address)))
    .check(status.is(200))
    .check(jsonPath("$.id").ofType[Long].saveAs("customerAddressId"))
    .check(jsonPath("$.name").ofType[String].is(address.name))
    .check(jsonPath("$.region.id").ofType[Long].is(address.regionId))
    .check(jsonPath("$.address1").ofType[String].is(address.address1))
    .check(jsonPath("$.address2").ofType[String].is(address.address2))
    .check(jsonPath("$.city").ofType[String].is(address.city))
    .check(jsonPath("$.zip").ofType[String].is(address.zip))
    .check(jsonPath("$.isDefault").ofType[Boolean].is(false))

  def update(address: AddressFixture): HttpRequestBuilder = http("Update My Address")
    .patch("/v1/my/addresses/${customerAddressId}")
    .body(StringBody(Utils.addressPayloadBody(address)))
    .check(status.is(200))
    .check(jsonPath("$.id").ofType[Long].is("${customerAddressId}"))
    .check(jsonPath("$.name").ofType[String].is(address.name))
    .check(jsonPath("$.region.id").ofType[Long].is(address.regionId))
    .check(jsonPath("$.address1").ofType[String].is(address.address1))
    .check(jsonPath("$.address2").ofType[String].is(address.address2))
    .check(jsonPath("$.city").ofType[String].is(address.city))
    .check(jsonPath("$.zip").ofType[String].is(address.zip))
    .check(jsonPath("$.zip").ofType[String].is(address.zip))
    .check(jsonPath("$.isDefault").ofType[Boolean].is(false))

  def setAsDefault(address: AddressFixture): HttpRequestBuilder = http("Set My Address As Default")
    .post("/v1/my/addresses/${customerAddressId}/default")
    .check(status.is(200))
    .check(jsonPath("$.id").ofType[Long].is("${customerAddressId}"))
    .check(jsonPath("$.name").ofType[String].is(address.name))
    .check(jsonPath("$.region.id").ofType[Long].is(address.regionId))
    .check(jsonPath("$.address1").ofType[String].is(address.address1))
    .check(jsonPath("$.address2").ofType[String].is(address.address2))
    .check(jsonPath("$.city").ofType[String].is(address.city))
    .check(jsonPath("$.zip").ofType[String].is(address.zip))
    .check(jsonPath("$.zip").ofType[String].is(address.zip))
    .check(jsonPath("$.isDefault").ofType[Boolean].is(true))

  def get(address: AddressFixture): HttpRequestBuilder = http("Get My Address")
    .get("/v1/my/addresses/${customerAddressId}")
    .check(status.is(200))
    .check(jsonPath("$.id").ofType[Long].is("${customerAddressId}"))
    .check(jsonPath("$.name").ofType[String].is(address.name))
    .check(jsonPath("$.region.id").ofType[Long].is(address.regionId))
    .check(jsonPath("$.address1").ofType[String].is(address.address1))
    .check(jsonPath("$.address2").ofType[String].is(address.address2))
    .check(jsonPath("$.city").ofType[String].is(address.city))
    .check(jsonPath("$.zip").ofType[String].is(address.zip))
    .check(jsonPath("$.zip").ofType[String].is(address.zip))

  def removeDefault(): HttpRequestBuilder = http("Reset My Default Address")
    .delete("/v1/my/addresses/default")
    .check(status.is(204))

  def delete(): HttpRequestBuilder = http("Delete My Address")
    .delete("/v1/my/addresses/${customerAddressId}")
    .check(status.is(204))
}
