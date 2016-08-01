package com.foxcommerce.perf.endpoints.write.storefront

import com.foxcommerce.common.Config
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder

object AccountEndpoint {

  def get(): HttpRequestBuilder = http("Get My Account")
    .get("/api/v1/my/account")
    .check(status.is(200))
    .check(jsonPath("$.id").ofType[Long].saveAs("accountId"))
}
