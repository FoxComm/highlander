package com.foxcommerce.endpoints

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder

import com.foxcommerce.common.Config
import com.foxcommerce.fixtures._

object SearchEndpoint {

  def checkCustomer(conf: Config, customer: CustomerFixture): HttpRequestBuilder = {
    http("Check Customer Presence in Elasticsearch")
      .get(s"${conf.elasticUrl}/${conf.indexName}" ++ "/customers_search_view/${customerId}")
      .check(status.is(200))
      .check(jsonPath("$._source.name").ofType[String].is(customer.name))
      .check(jsonPath("$._source.email").ofType[String].is("${customerEmail}"))
      .check(jsonPath("$._source.isBlacklisted").ofType[Boolean].is(customer.isBlacklisted))
      .check(jsonPath("$._source.isDisabled").ofType[Boolean].is(customer.isDisabled))
      .check(jsonPath("$._source.shippingAddresses[0].address1").ofType[String].is(customer.address.address1))
      .check(jsonPath("$._source.shippingAddresses[0].address2").ofType[String].is(customer.address.address2))
      .check(jsonPath("$._source.shippingAddresses[0].city").ofType[String].is(customer.address.city))
      .check(jsonPath("$._source.shippingAddresses[0].zip").ofType[String].is(customer.address.zip))
      .check(jsonPath("$._source.storeCreditCount").ofType[Long].is(customer.storeCreditCount))
      .check(jsonPath("$._source.storeCreditTotal").ofType[Long].is(customer.storeCreditTotal))
  }

  def checkStoreCredit(conf: Config, storeCredit: StoreCreditFixture, state: String): HttpRequestBuilder = {
    http("Check Store Credit Presence in Elasticsearch")
      .get(s"${conf.elasticUrl}/${conf.indexName}" ++ "/store_credits_search_view/${storeCreditId}")
      .check(status.is(200))
      .check(jsonPath("$._source.status").ofType[String].is(state))
      .check(jsonPath("$._source.originalBalance").ofType[Long].is(storeCredit.amount))
  }

  def checkGiftCard(conf: Config, giftCard: GiftCardFixture, state: String): HttpRequestBuilder = {
    http("Check Gift Card Presence in Elasticsearch")
      .get(s"${conf.elasticUrl}/${conf.indexName}" ++ "/gift_cards_search_view/${giftCardId}")
      .check(status.is(200))
      .check(jsonPath("$._source.status").ofType[String].is(state))
      .check(jsonPath("$._source.originalBalance").ofType[Long].is(giftCard.balance))
  }

  def checkOrder(conf: Config, order: OrderFixture, state: String): HttpRequestBuilder = {
    http("Check Order Presence in Elasticsearch")
      .get(s"${conf.elasticUrl}/${conf.indexName}" ++ "/orders_search_view/${orderId}")
      .check(status.is(200))
      .check(jsonPath("$._source.state").ofType[String].is(state))
      .check(jsonPath("$._source.shippingAddresses[0].address1").ofType[String].is(order.shippingAddress.address1))
      .check(jsonPath("$._source.shippingAddresses[0].address2").ofType[String].is(order.shippingAddress.address2))
      .check(jsonPath("$._source.shippingAddresses[0].city").ofType[String].is(order.shippingAddress.city))
      .check(jsonPath("$._source.shippingAddresses[0].zip").ofType[String].is(order.shippingAddress.zip))
  }
}
