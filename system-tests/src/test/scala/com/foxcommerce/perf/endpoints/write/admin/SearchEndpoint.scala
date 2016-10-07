package com.foxcommerce.perf.endpoints.write.admin

import com.foxcommerce.common.Config
import com.foxcommerce.fixtures._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder

object SearchEndpoint {

  def checkCustomer(conf: Config, customer: CustomerFixture): HttpRequestBuilder = {
    http("Check Customer Presence in Elasticsearch")
      .get("/api/search/admin/customers_search_view/${customerId}")
      .check(status.is(200))
  }

  def checkStoreCredit(conf: Config, storeCredit: StoreCreditFixture, state: String): HttpRequestBuilder = {
    http("Check Store Credit Presence in Elasticsearch")
      .get("/api/search/admin/store_credits_search_view/${storeCreditId}")
      .check(status.is(200))
  }

  def checkGiftCard(conf: Config, giftCard: GiftCardFixture, state: String): HttpRequestBuilder = {
    http("Check Gift Card Presence in Elasticsearch")
      .get("/api/search/admin/gift_cards_search_view/${giftCardId}")
      .check(status.is(200))
  }

  def checkOrder(conf: Config, order: OrderFixture, state: String): HttpRequestBuilder = {
    http("Check Order Presence in Elasticsearch")
      .get("/api/search/admin/orders_search_view/_search?q=referenceNumber:${orderRefNum}")
      .check(status.is(200))
  }
}
