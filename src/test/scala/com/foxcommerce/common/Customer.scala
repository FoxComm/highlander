package com.foxcommerce.common

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder

object Customer {

  val typeName = "customers_search_view"

  def assert(conf: Config, name: String, isBlacklisted: Boolean, isDisabled: Boolean): HttpRequestBuilder = {
    http("Assert Customer Presence in Elasticsearch")
      .get(s"${conf.elasticUrl}/${conf.indexName}/$typeName/" ++ "${customerId}")
      .check(status.is(200))
      .check(jsonPath("$._source.name").ofType[String].is(name))
      .check(jsonPath("$._source.isBlacklisted").ofType[Boolean].is(isBlacklisted))
      .check(jsonPath("$._source.isDisabled").ofType[Boolean].is(isDisabled))
  }

  def create(name: String, email: String): HttpRequestBuilder = {
    http("Create Customer")
      .post("/v1/customers")
      .basicAuth("${email}", "${password}")
      .body(StringBody("""{"name": "%s", "email": "%s"}""".format(name, email)))
      .check(status.is(200))
      .check(jsonPath("$.id").ofType[Long].saveAs("customerId"))
      .check(jsonPath("$.name").ofType[String].is(name))
  }

  def update(name: String): HttpRequestBuilder = {
    http("Update Customer")
      .patch("/v1/customers/${customerId}")
      .basicAuth("${email}", "${password}")
      .body(StringBody("""{"name": "%s"}""".format(name)))
      .check(status.is(200))
      .check(jsonPath("$.id").ofType[Long].is("${customerId}"))
      .check(jsonPath("$.name").ofType[String].is(name))
      .check(jsonPath("$.email").ofType[String].is(email))
  }

  def blacklist(isBlacklisted: Boolean): HttpRequestBuilder = {
    http("Toggle Customer Blacklisted Flag")
      .post("/v1/customers/${customerId}/blacklist")
      .basicAuth("${email}", "${password}")
      .body(StringBody("""{"blacklisted": %b}""".format(isBlacklisted)))
      .check(status.is(200))
      .check(jsonPath("$.isBlacklisted").ofType[Boolean].is(isBlacklisted))
  }

  def disable(isDisabled: Boolean): HttpRequestBuilder = {
    http("Toggle Customer Disabled Flag")
      .post("/v1/customers/${customerId}/disable")
      .basicAuth("${email}", "${password}")
      .body(StringBody("""{"disabled": %b}""".format(isDisabled)))
      .check(status.is(200))
      .check(jsonPath("$.disabled").ofType[Boolean].is(isDisabled))
  }
}