package com.foxcommerce.common

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder

object Customer {

  def blacklistAdd(config: Config): HttpRequestBuilder = {
    http("Add Customer To Blacklist")
      .post("/v1/customers/1/blacklist")
      .basicAuth("${username}", "${password}")
      .body(StringBody("""{"blacklisted": true}"""))
      .check(status.is(200))
      .check(jsonPath("$.isBlacklisted").ofType[Boolean].is(true))
  }

  def blacklistRemove(config: Config): HttpRequestBuilder = {
    http("Remove Customer From Blacklist")
      .post("/v1/customers/1/blacklist")
      .basicAuth("${username}", "${password}")
      .body(StringBody("""{"blacklisted": false}"""))
      .check(status.is(200))
      .check(jsonPath("$.isBlacklisted").ofType[Boolean].is(false))
  }

  def blacklistAssert(config: Config, isBlacklisted: Boolean): HttpRequestBuilder = {
    http("Check Blacklisted Customer in Elasticsearch")
      .get(config.elasticUrl + "/phoenix/customers_search_view/1")
      .check(status.is(200))
      .check(jsonPath("$._source.isBlacklisted").ofType[Boolean].is(isBlacklisted))
  }
}