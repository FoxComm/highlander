package com.foxcommerce.endpoints.storefront

import com.foxcommerce.common.Config
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.action.AddCookieBuilder
import io.gatling.http.request.builder.HttpRequestBuilder

object CustomerActivity {

  def asCustomer(): AddCookieBuilder = addCookie(Cookie(Config.jwtCookie, "${jwtTokenCustomer}"))

  def get(): HttpRequestBuilder = http("Get My Account")
    .get("/v1/my/account")
    .check(status.is(200))
    .check(jsonPath("$.id").ofType[Long].saveAs("accountId"))
    .check(jsonPath("$.email").ofType[String].is("${email}"))
}
