package com.foxcommerce.endpoints.storefront

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder

object AccountEndpoint {

  val header = "JWT"

  def get(): HttpRequestBuilder = http("Get My Account")
    .get("/v1/my/account")
    .header(header, "${jwtTokenCustomer}")
    .check(status.is(200))
    .check(jsonPath("$.id").ofType[Long].saveAs("accountId"))
    .check(jsonPath("$.email").ofType[String].is("${email}"))
}
