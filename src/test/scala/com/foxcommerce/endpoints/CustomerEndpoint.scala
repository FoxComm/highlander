package com.foxcommerce.endpoints

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder

import com.foxcommerce.payloads.CustomerPayload

object CustomerEndpoint {

  def create(customer: CustomerPayload): HttpRequestBuilder = {
    val requestBody = """{"name": "%s", "email": "%s"}""".format(customer.name, customer.email)

    http("Create Customer")
      .post("/v1/customers")
      .basicAuth("${email}", "${password}")
      .body(StringBody(requestBody))
      .check(status.is(200))
      .check(jsonPath("$.id").ofType[Long].saveAs("customerId"))
      .check(jsonPath("$.name").ofType[String].is(customer.name))
      .check(jsonPath("$.email").ofType[String].is(customer.email))
  }

  def update(customer: CustomerPayload): HttpRequestBuilder = http("Update Customer")
    .patch("/v1/customers/${customerId}")
    .basicAuth("${email}", "${password}")
    .body(StringBody("""{"name": "%s"}""".format(customer.name)))
    .check(status.is(200))
    .check(jsonPath("$.id").ofType[Long].is("${customerId}"))
    .check(jsonPath("$.name").ofType[String].is(customer.name))
    .check(jsonPath("$.email").ofType[String].is(customer.email))

  def blacklist(customer: CustomerPayload): HttpRequestBuilder = http("Toggle Customer Blacklisted Flag")
    .post("/v1/customers/${customerId}/blacklist")
    .basicAuth("${email}", "${password}")
    .body(StringBody("""{"blacklisted": %b}""".format(customer.isBlacklisted)))
    .check(status.is(200))
    .check(jsonPath("$.isBlacklisted").ofType[Boolean].is(customer.isBlacklisted))

  def disable(customer: CustomerPayload): HttpRequestBuilder = http("Toggle Customer Disabled Flag")
    .post("/v1/customers/${customerId}/disable")
    .basicAuth("${email}", "${password}")
    .body(StringBody("""{"disabled": %b}""".format(customer.isBlacklisted)))
    .check(status.is(200))
    .check(jsonPath("$.disabled").ofType[Boolean].is(customer.isBlacklisted))
}
