package com.foxcommerce.perf.endpoints.write.storefront

import com.foxcommerce.common.Config
import com.foxcommerce.fixtures._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder

object CreditCardEndpoint {

  def create(card: CreditCardFixture, address: AddressFixture): HttpRequestBuilder = http("Create My Credit Card")
    .post("/api/v1/my/payment-methods/credit-cards")
    .body(ELFileBody("request-bodies/credit_card.json"))
    .check(status.is(200))
    .check(jsonPath("$.id").ofType[Long].saveAs("creditCardId"))

  def update(card: CreditCardFixture, address: AddressFixture): HttpRequestBuilder = http("Update My Credit Card")
    .patch("/api/v1/my/payment-methods/credit-cards/${creditCardId}")
    .body(StringBody("""{"holderName": "%s"}""".format(card.holderName)))
    .check(status.is(200))
    .check(jsonPath("$.id").ofType[Long].saveAs("creditCardId"))

  def setAsDefault(card: CreditCardFixture, address: AddressFixture): HttpRequestBuilder =
    http("Set My Credit Card As Default")
      .post("/api/v1/my/payment-methods/credit-cards/${creditCardId}/default")
      .body(StringBody("""{"isDefault": true}"""))
      .check(status.is(200))

  def get(card: CreditCardFixture, address: AddressFixture): HttpRequestBuilder =
    http("Get My Credit Card")
      .get("/api/v1/my/payment-methods/credit-cards/${creditCardId}")
      .check(status.is(200))

  def delete(): HttpRequestBuilder = http("Delete My Credit Card")
    .delete("/api/v1/my/payment-methods/credit-cards/${creditCardId}")
    .check(status.is(204))
}
