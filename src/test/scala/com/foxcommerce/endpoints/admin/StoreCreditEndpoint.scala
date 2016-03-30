package com.foxcommerce.endpoints.admin

import com.foxcommerce.fixtures.StoreCreditFixture
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder

object StoreCreditEndpoint {

  val header = "JWT"
  val originType = "csrAppeasement"
  val cancellationReasonId = 1

  def create(payload: StoreCreditFixture): HttpRequestBuilder = http("Create Store Credit For Customer")
    .post("/v1/customers/${customerId}/payment-methods/store-credit")
    .header(header, "${jwtTokenAdmin}")
    .body(StringBody("""{"amount": %d, "reasonId": %d}""".format(payload.amount, payload.reasonId)))
    .check(status.is(200))
    .check(jsonPath("$.id").ofType[Long].saveAs("storeCreditId"))
    .check(jsonPath("$.originType").ofType[String].is(originType))
    .check(jsonPath("$.state").ofType[String].is("active"))
    .check(jsonPath("$.originalBalance").ofType[Long].is(payload.amount))
    .check(jsonPath("$.currentBalance").ofType[Long].is(payload.amount))
    .check(jsonPath("$.availableBalance").ofType[Long].is(payload.amount))

  def cancel(): HttpRequestBuilder = http("Cancel Store Credit")
    .patch("/v1/store-credits/${storeCreditId}")
    .header(header, "${jwtTokenAdmin}")
    .body(StringBody("""{"state": "canceled", "reasonId": %d}""".format(cancellationReasonId)))
    .check(status.is(200))
    .check(jsonPath("$.state").ofType[String].is("canceled"))
}
