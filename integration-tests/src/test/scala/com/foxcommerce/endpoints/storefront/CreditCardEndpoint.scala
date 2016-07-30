package com.foxcommerce.endpoints.storefront

import com.foxcommerce.common.Config
import com.foxcommerce.fixtures._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder

object CreditCardEndpoint {

  def create(card: CreditCardFixture, address: AddressFixture): HttpRequestBuilder = http("Create My Credit Card")
    .post("/v1/my/payment-methods/credit-cards")
    .body(ELFileBody("request-bodies/credit_card.json"))
    .check(status.is(200))
    .check(jsonPath("$.id").ofType[Long].saveAs("creditCardId"))
    .check(jsonPath("$.customerId").ofType[Long].is("${accountId}"))
    .check(jsonPath("$.holderName").ofType[String].is(card.holderName))
    .check(jsonPath("$.expMonth").ofType[Int].is(card.expMonth))
    .check(jsonPath("$.expYear").ofType[Int].is(card.expYear))
    .check(jsonPath("$.isDefault").ofType[Boolean].is(false))
    .check(jsonPath("$.address.name").ofType[String].is(address.name))
    .check(jsonPath("$.address.region.id").ofType[Long].is(address.regionId))
    .check(jsonPath("$.address.address1").ofType[String].is(address.address1))
    .check(jsonPath("$.address.address2").ofType[String].is(address.address2))
    .check(jsonPath("$.address.city").ofType[String].is(address.city))
    .check(jsonPath("$.address.zip").ofType[String].is(address.zip))

  def update(card: CreditCardFixture, address: AddressFixture): HttpRequestBuilder = http("Update My Credit Card")
    .patch("/v1/my/payment-methods/credit-cards/${creditCardId}")
    .body(StringBody("""{"holderName": "%s"}""".format(card.holderName)))
    .check(status.is(200))
    .check(jsonPath("$.id").ofType[Long].saveAs("creditCardId"))
    .check(jsonPath("$.customerId").ofType[Long].is("${accountId}"))
    .check(jsonPath("$.holderName").ofType[String].is(card.holderName))
    .check(jsonPath("$.expMonth").ofType[Int].is(card.expMonth))
    .check(jsonPath("$.expYear").ofType[Int].is(card.expYear))
    .check(jsonPath("$.isDefault").ofType[Boolean].is(false))
    .check(jsonPath("$.address.name").ofType[String].is(address.name))
    .check(jsonPath("$.address.region.id").ofType[Long].is(address.regionId))
    .check(jsonPath("$.address.address1").ofType[String].is(address.address1))
    .check(jsonPath("$.address.address2").ofType[String].is(address.address2))
    .check(jsonPath("$.address.city").ofType[String].is(address.city))
    .check(jsonPath("$.address.zip").ofType[String].is(address.zip))

  def setAsDefault(card: CreditCardFixture, address: AddressFixture): HttpRequestBuilder =
    http("Set My Credit Card As Default")
      .post("/v1/my/payment-methods/credit-cards/${creditCardId}/default")
      .body(StringBody("""{"isDefault": true}"""))
      .check(status.is(200))
      .check(jsonPath("$.id").ofType[Long].is("${creditCardId}"))
      .check(jsonPath("$.customerId").ofType[Long].is("${accountId}"))
      .check(jsonPath("$.holderName").ofType[String].is(card.holderName))
      .check(jsonPath("$.expMonth").ofType[Int].is(card.expMonth))
      .check(jsonPath("$.expYear").ofType[Int].is(card.expYear))
      .check(jsonPath("$.isDefault").ofType[Boolean].is(true))
      .check(jsonPath("$.address.name").ofType[String].is(address.name))
      .check(jsonPath("$.address.region.id").ofType[Long].is(address.regionId))
      .check(jsonPath("$.address.address1").ofType[String].is(address.address1))
      .check(jsonPath("$.address.address2").ofType[String].is(address.address2))
      .check(jsonPath("$.address.city").ofType[String].is(address.city))
      .check(jsonPath("$.address.zip").ofType[String].is(address.zip))

  def get(card: CreditCardFixture, address: AddressFixture): HttpRequestBuilder =
    http("Get My Credit Card")
      .get("/v1/my/payment-methods/credit-cards/${creditCardId}")
      .check(status.is(200))
      .check(jsonPath("$.id").ofType[Long].is("${creditCardId}"))
      .check(jsonPath("$.customerId").ofType[Long].is("${accountId}"))
      .check(jsonPath("$.holderName").ofType[String].is(card.holderName))
      .check(jsonPath("$.expMonth").ofType[Int].is(card.expMonth))
      .check(jsonPath("$.expYear").ofType[Int].is(card.expYear))
      .check(jsonPath("$.isDefault").ofType[Boolean].is(false))
      .check(jsonPath("$.address.name").ofType[String].is(address.name))
      .check(jsonPath("$.address.region.id").ofType[Long].is(address.regionId))
      .check(jsonPath("$.address.address1").ofType[String].is(address.address1))
      .check(jsonPath("$.address.address2").ofType[String].is(address.address2))
      .check(jsonPath("$.address.city").ofType[String].is(address.city))
      .check(jsonPath("$.address.zip").ofType[String].is(address.zip))

  def delete(): HttpRequestBuilder = http("Delete My Credit Card")
    .delete("/v1/my/payment-methods/credit-cards/${creditCardId}")
    .check(status.is(204))
}
