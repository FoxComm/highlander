package com.foxcommerce.perf.endpoints.write.admin

import com.foxcommerce.common.Config
import com.foxcommerce.fixtures.CustomerFixture
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder

object CustomerEndpoint {

  def create(customer: CustomerFixture): HttpRequestBuilder = {
    val requestBody = """{"name": "%s", "email": "${customerEmail}"}""".format(customer.name)

    http("Create Customer")
      .post("/api/v1/customers")
      .body(StringBody(requestBody))
      .check(status.is(200))
      .check(jsonPath("$.id").ofType[Long].saveAs("customerId"))
  }

  def update(customer: CustomerFixture): HttpRequestBuilder = http("Update Customer")
    .patch("/api/v1/customers/${customerId}")
    .body(StringBody("""{"name": "%s"}""".format(customer.name)))
    .check(status.is(200))

  def blacklist(customer: CustomerFixture): HttpRequestBuilder = http("Toggle Customer Blacklisted Flag")
    .post("/api/v1/customers/${customerId}/blacklist")
    .body(StringBody("""{"blacklisted": %b}""".format(customer.isBlacklisted)))
    .check(status.is(200))

  def disable(customer: CustomerFixture): HttpRequestBuilder = http("Toggle Customer Disabled Flag")
    .post("/api/v1/customers/${customerId}/disable")
    .body(StringBody("""{"disabled": %b}""".format(customer.isBlacklisted)))
    .check(status.is(200))
}
