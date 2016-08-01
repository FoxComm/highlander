package com.foxcommerce.endpoints.admin

import com.foxcommerce.common.Config
import com.foxcommerce.fixtures.GiftCardFixture
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder

object GiftCardEndpoint {

  val originType = "csrAppeasement"
  val cancellationReasonId = 1

  def create(payload: GiftCardFixture): HttpRequestBuilder = http("Create Gift Card")
    .post("/v1/gift-cards")
    .body(StringBody("""{"balance": %d, "reasonId": %d, "quantity": 1}""".format(payload.balance, payload.reasonId)))
    .check(status.is(200))
    .check(jsonPath("$[0].giftCard.id").ofType[Long].saveAs("giftCardId"))
    .check(jsonPath("$[0].giftCard.code").ofType[String].saveAs("giftCardCode"))
    .check(jsonPath("$[0].giftCard.originType").ofType[String].is(originType))
    .check(jsonPath("$[0].giftCard.state").ofType[String].is("active"))
    .check(jsonPath("$[0].giftCard.originalBalance").ofType[Long].is(payload.balance))
    .check(jsonPath("$[0].giftCard.currentBalance").ofType[Long].is(payload.balance))
    .check(jsonPath("$[0].giftCard.availableBalance").ofType[Long].is(payload.balance))

  def bulkCreate(): HttpRequestBuilder = http("Bulk Create 20 Gift Cards")
    .post("/v1/gift-cards")
    .body(StringBody("""{"balance": 10, "reasonId": 1, "quantity": 20}"""))
    .check(status.is(200))
    .check(jsonPath("$..giftCard[?(@.state == 'active')]").count.is(20))
    .check(jsonPath("$..giftCard.code").findAll.saveAs("gcCodesHeap"))

  def bulkWatch(): HttpRequestBuilder = http("Watch ${gcPortionCount}")
    .post("/v1/gift-cards/watchers")
    .body(StringBody("""{"giftCardCodes": [${gcCodesPortion}], "watcherId": 1}"""))
    .check(status.is(200))
    .check(jsonPath("$.errors").count.is(0))

  def bulkUnwatch(): HttpRequestBuilder = http("Unwatch")
    .post("/v1/gift-cards/watchers/delete")
    .body(StringBody("""{"giftCardCodes": [${gcCodesPortion}], "watcherId": 1}"""))
    .check(status.is(200))
    .check(jsonPath("$.errors").count.is(0))
    .silent

  def cancel(): HttpRequestBuilder = http("Cancel Gift Card")
    .patch("/v1/gift-cards/${giftCardCode}")
    .body(StringBody("""{"state": "canceled", "reasonId": %d}""".format(cancellationReasonId)))
    .check(status.is(200))
    .check(jsonPath("$.state").ofType[String].is("canceled"))
}
