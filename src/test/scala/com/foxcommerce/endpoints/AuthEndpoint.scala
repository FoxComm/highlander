package com.foxcommerce.endpoints

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder

object AuthEndpoint {

  def loginAsAdmin(): HttpRequestBuilder = http("Login as Admin")
    .post("/v1/public/login")
    .body(ELFileBody("request-bodies/login_as_admin.json"))
    .check(status.is(200))
    .check(header("JWT").saveAs("jwtTokenAdmin"))

  def loginAsCustomer(): HttpRequestBuilder = http("Login as Customer")
    .post("/v1/public/login")
    .body(ELFileBody("request-bodies/login_as_customer.json"))
    .check(status.is(200))
    .check(header("JWT").saveAs("jwtTokenCustomer"))

  def loginAsIntruder(): HttpRequestBuilder = http("Login as Intruder")
    .post("/v1/public/login")
    .body(ELFileBody("request-bodies/login_as_intruder.json"))
    .check(status.is(200))
    .check(header("JWT").saveAs("jwtTokenIntruder"))
}
