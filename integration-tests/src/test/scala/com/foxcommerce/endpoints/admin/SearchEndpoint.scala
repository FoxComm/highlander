package com.foxcommerce.endpoints.admin

import com.foxcommerce.common.Config
import com.foxcommerce.fixtures._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder

object SearchEndpoint {

  def checkCustomer(conf: Config, customer: CustomerFixture): HttpRequestBuilder = {
    http("Check Customer Presence in Elasticsearch")
      .get("/search/admin/customers_search_view/${customerId}")
      .check(status.is(200))
      .check(jsonPath("$.name").ofType[String].is(customer.name))
      .check(jsonPath("$.email").ofType[String].is("${customerEmail}"))
      .check(jsonPath("$.isBlacklisted").ofType[Boolean].is(customer.isBlacklisted))
      .check(jsonPath("$.isDisabled").ofType[Boolean].is(customer.isDisabled))
      .check(jsonPath("$.shippingAddresses[0].address1").ofType[String].is(customer.address.address1))
      .check(jsonPath("$.shippingAddresses[0].address2").ofType[String].is(customer.address.address2))
      .check(jsonPath("$.shippingAddresses[0].city").ofType[String].is(customer.address.city))
      .check(jsonPath("$.shippingAddresses[0].zip").ofType[String].is(customer.address.zip))
      .check(jsonPath("$.storeCreditCount").ofType[Long].is(customer.storeCreditCount))
      .check(jsonPath("$.storeCreditTotal").ofType[Long].is(customer.storeCreditTotal))
  }

  def checkStoreCredit(conf: Config, storeCredit: StoreCreditFixture, state: String): HttpRequestBuilder = {
    http("Check Store Credit Presence in Elasticsearch")
      .get("/search/admin/store_credits_search_view/${storeCreditId}")
      .check(status.is(200))
      .check(jsonPath("$.state").ofType[String].is(state))
      .check(jsonPath("$.originalBalance").ofType[Long].is(storeCredit.amount))
  }

  def checkGiftCard(conf: Config, giftCard: GiftCardFixture, state: String): HttpRequestBuilder = {
    http("Check Gift Card Presence in Elasticsearch")
      .get("/search/admin/gift_cards_search_view/${giftCardId}")
      .check(status.is(200))
      .check(jsonPath("$.state").ofType[String].is(state))
      .check(jsonPath("$.originalBalance").ofType[Long].is(giftCard.balance))
  }

  def checkOrder(conf: Config, order: OrderFixture, state: String): HttpRequestBuilder = {
    http("Check Order Presence in Elasticsearch")
      .get("/search/admin/orders_search_view/_search?q=referenceNumber:${orderRefNum}")
      .check(status.is(200))
      // TODO: FIXME!
      //.check(jsonPath("$.result[0].state").ofType[String].is(state))
      //.check(jsonPath("$.result[0].shippingAddresses[0].address1").ofType[String].is(order.shippingAddress.address1))
      //.check(jsonPath("$.result[0].shippingAddresses[0].address2").ofType[String].is(order.shippingAddress.address2))
      //.check(jsonPath("$.result[0].shippingAddresses[0].city").ofType[String].is(order.shippingAddress.city))
      //.check(jsonPath("$.result[0].shippingAddresses[0].zip").ofType[String].is(order.shippingAddress.zip))
  }
}
