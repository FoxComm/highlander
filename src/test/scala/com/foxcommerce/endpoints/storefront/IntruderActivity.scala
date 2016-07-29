package com.foxcommerce.endpoints.storefront

import com.foxcommerce.common._
import com.foxcommerce.fixtures._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.action.AddCookieBuilder
import io.gatling.http.request.builder.HttpRequestBuilder

object IntruderActivity {

  def asIntruder(): AddCookieBuilder = addCookie(Cookie(Config.jwtCookie, "${jwtTokenIntruder}"))

  def register(): HttpRequestBuilder = http("Intruder Registration")
    .post("/v1/public/registrations/new")
    .body(ELFileBody("request-bodies/intruder_registration.json"))
    .check(status.is(200))
    .check(jsonPath("$.id").ofType[Long].saveAs("${intuderId}"))
    .check(jsonPath("$.email").ofType[String].is("${intruderEmail}"))

  object Address {

    def get(): HttpRequestBuilder = http("Attempt to Get Another Customer's Address")
      .get("/v1/my/addresses/${customerAddressId}")
      .check(status.is(404))

    def update(address: AddressFixture): HttpRequestBuilder = http("Attempt to Update Another Customer's Address")
      .patch("/v1/my/addresses/${customerAddressId}")
      .body(StringBody(Utils.addressPayloadBody(address)))
      .check(status.is(404))

    def setAsDefault(): HttpRequestBuilder = http("Attempt to Set Another Customer's Address As Default")
      .post("/v1/my/addresses/${customerAddressId}/default")
      .check(status.is(404))

    def delete(): HttpRequestBuilder = http("Attempt to Delete Another Customer's Address")
      .delete("/v1/my/addresses/${customerAddressId}")
      .check(status.is(404))
  }

  object Cart {

    def touch(): HttpRequestBuilder = http("Intruder Cart Touch")
      .get("/v1/my/cart")
      .check(status.is(200))

    def shippingAddressAdd(): HttpRequestBuilder = http("Attempt To Use Another Customer's Address As Shipping Address")
      .patch("/v1/my/cart/shipping-address/${customerAddressId}")
      .check(status.is(404))
  }

  object CreditCard {

    def get(): HttpRequestBuilder = http("Attempt to Get Another Customer's Credit Card")
      .get("/v1/my/payment-methods/credit-cards/${creditCardId}")
      .check(status.is(404))

    def create(): HttpRequestBuilder = http("Attempt to Create Credit Card with Another Customer's Address")
      .post("/v1/my/payment-methods/credit-cards")
      .body(ELFileBody("request-bodies/credit_card.json"))
      .check(status.is(404))

    def update(address: AddressFixture): HttpRequestBuilder = http("Attempt to Update Another Customer's Credit Card")
      .patch("/v1/my/payment-methods/credit-cards/${creditCardId}")
      .body(StringBody(Utils.addressPayloadBody(address)))
      .check(status.is(404))

    def setAsDefault(): HttpRequestBuilder = http("Attempt to Set Another Customer's Credit Card As Default")
      .post("/v1/my/payment-methods/credit-cards/${creditCardId}/default")
      .check(status.is(404))

    def delete(): HttpRequestBuilder = http("Attempt to Delete Another Customer's Credit Card")
      .delete("/v1/my/payment-methods/credit-cards/${creditCardId}")
      .check(status.is(404))
  }
}
