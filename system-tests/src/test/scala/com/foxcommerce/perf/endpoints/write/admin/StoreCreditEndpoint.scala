package com.foxcommerce.perf.endpoints.write.admin

import com.foxcommerce.common.Config
import com.foxcommerce.fixtures.StoreCreditFixture
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder

object StoreCreditEndpoint {

  val originType = "csrAppeasement"
  val cancellationReasonId = 1

  def create(payload: StoreCreditFixture): HttpRequestBuilder = http("Create Store Credit For Customer")
    .post("/api/v1/customers/${customerId}/payment-methods/store-credit")
    .body(StringBody("""{"amount": %d, "reasonId": %d}""".format(payload.amount, payload.reasonId)))
    .check(status.is(200))
    .check(jsonPath("$.id").ofType[Long].saveAs("storeCreditId"))

  def cancel(): HttpRequestBuilder = http("Cancel Store Credit")
    .patch("/api/v1/store-credits/${storeCreditId}")
    .body(StringBody("""{"state": "canceled", "reasonId": %d}""".format(cancellationReasonId)))
    .check(status.is(200))
}
