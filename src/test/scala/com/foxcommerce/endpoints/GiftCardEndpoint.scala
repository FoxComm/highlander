package com.foxcommerce.endpoints

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder

import com.foxcommerce.fixtures.GiftCardFixture

object GiftCardEndpoint {

  val originType = "csrAppeasement"
  val cancellationReasonId = 1

  def create(payload: GiftCardFixture): HttpRequestBuilder = http("Create Gift Card")
    .post("/v1/gift-cards")
    .basicAuth("${email}", "${password}")
    .body(StringBody("""{"balance": %d, "reasonId": %d, "quantity": 1}""".format(payload.balance, payload.reasonId)))
    .check(status.is(200))
    .check(jsonPath("$[0].giftCard.id").ofType[Long].saveAs("giftCardId"))
    .check(jsonPath("$[0].giftCard.code").ofType[String].saveAs("giftCardCode"))
    .check(jsonPath("$[0].giftCard.originType").ofType[String].is(originType))
    .check(jsonPath("$[0].giftCard.status").ofType[String].is("active"))
    .check(jsonPath("$[0].giftCard.originalBalance").ofType[Long].is(payload.balance))
    .check(jsonPath("$[0].giftCard.currentBalance").ofType[Long].is(payload.balance))
    .check(jsonPath("$[0].giftCard.availableBalance").ofType[Long].is(payload.balance))

  def cancel(): HttpRequestBuilder = http("Cancel Gift Card")
    .patch("/v1/gift-cards/${giftCardCode}")
    .basicAuth("${email}", "${password}")
    .body(StringBody("""{"status": "canceled", "reasonId": %d}""".format(cancellationReasonId)))
    .check(status.is(200))
    .check(jsonPath("$.status").ofType[String].is("canceled"))
}
