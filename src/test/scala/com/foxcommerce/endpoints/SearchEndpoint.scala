package com.foxcommerce.endpoints

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder

import com.foxcommerce.common.Config
import com.foxcommerce.payloads.{StoreCreditPayload, CustomerPayload}

object SearchEndpoint {

  def checkCustomer(conf: Config, customer: CustomerPayload): HttpRequestBuilder = {
    http("Assert Customer Presence in Elasticsearch")
      .get(s"${conf.elasticUrl}/${conf.indexName}" ++ "/customers_search_view/${customerId}")
      .check(status.is(200))
      .check(jsonPath("$._source.name").ofType[String].is(customer.name))
      .check(jsonPath("$._source.email").ofType[String].is(customer.email))
      .check(jsonPath("$._source.isBlacklisted").ofType[Boolean].is(customer.isBlacklisted))
      .check(jsonPath("$._source.isDisabled").ofType[Boolean].is(customer.isDisabled))
      .check(jsonPath("$._source.shippingAddresses[0].address1").ofType[String].is(customer.address.address1))
      .check(jsonPath("$._source.shippingAddresses[0].address2").ofType[String].is(customer.address.address2))
      .check(jsonPath("$._source.shippingAddresses[0].city").ofType[String].is(customer.address.city))
      .check(jsonPath("$._source.shippingAddresses[0].zip").ofType[String].is(customer.address.zip))
      .check(jsonPath("$._source.storeCreditCount").ofType[Long].is(customer.storeCreditCount))
      .check(jsonPath("$._source.storeCreditTotal").ofType[Long].is(customer.storeCreditTotal))
  }

  def checkStoreCredit(conf: Config, storeCredit: StoreCreditPayload, state: String): HttpRequestBuilder = {
    http("Assert Customer Presence in Elasticsearch")
      .get(s"${conf.elasticUrl}/${conf.indexName}" ++ "/store_credits_search_view/${storeCreditId}")
      .check(status.is(200))
      .check(jsonPath("$._source.status").ofType[String].is(state))
      .check(jsonPath("$._source.originalBalance").ofType[Long].is(storeCredit.amount))
  }
}
