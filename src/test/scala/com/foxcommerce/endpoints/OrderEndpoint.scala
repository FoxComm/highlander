package com.foxcommerce.endpoints

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder

import com.foxcommerce.payloads._
import com.foxcommerce.common._

object OrderEndpoint {

  def create(order: OrderPayload): HttpRequestBuilder = {
    http("Create Order")
      .post("/v1/orders")
      .basicAuth("${email}", "${password}")
      .body(StringBody("""{"customerId": ${customerId}}"""))
      .check(status.is(200))
      .check(jsonPath("$.id").ofType[Long].saveAs("orderId"))
      .check(jsonPath("$.referenceNumber").ofType[String].saveAs("orderRefNum"))
      .check(jsonPath("$.orderState").ofType[String].is("cart"))
      .check(jsonPath("$.customer.id").ofType[String].is("${customerId}"))
      .check(jsonPath("$.customer.name").ofType[String].is(order.customer.name))
  }

  def cancel(): HttpRequestBuilder = http("Cancel Order")
    .patch("/v1/orders/${orderRefNum}")
    .basicAuth("${email}", "${password}")
    .body(StringBody("""{"state": "canceled"}"""))
    .check(status.is(200))
    .check(jsonPath("$.orderState").ofType[String].is("canceled"))

  def addShippingAddress(address: AddressPayload): HttpRequestBuilder = {
    http("Add Order Shipping Address")
      .post("/v1/orders/${orderRefNum}/shipping-address")
      .basicAuth("${email}", "${password}")
      .body(StringBody(Utils.addressPayloadBody(address)))
      .check(status.is(200))
      .check(jsonPath("$.result.shippingAddress.name").ofType[String].is(address.name))
      .check(jsonPath("$.result.shippingAddress.region.id").ofType[Long].is(address.regionId))
      .check(jsonPath("$.result.shippingAddress.address1").ofType[String].is(address.address1))
      .check(jsonPath("$.result.shippingAddress.address2").ofType[String].is(address.address2))
      .check(jsonPath("$.result.shippingAddress.city").ofType[String].is(address.city))
      .check(jsonPath("$.result.shippingAddress.zip").ofType[String].is(address.zip))
  }

  def updateShippingAddress(address: AddressPayload): HttpRequestBuilder = {
    http("Update Order Shipping Address")
      .patch("/v1/orders/${orderRefNum}/shipping-address")
      .basicAuth("${email}", "${password}")
      .body(StringBody(Utils.addressPayloadBody(address)))
      .check(status.is(200))
      .check(jsonPath("$.result.shippingAddress.name").ofType[String].is(address.name))
      .check(jsonPath("$.result.shippingAddress.region.id").ofType[Long].is(address.regionId))
      .check(jsonPath("$.result.shippingAddress.address1").ofType[String].is(address.address1))
      .check(jsonPath("$.result.shippingAddress.address2").ofType[String].is(address.address2))
      .check(jsonPath("$.result.shippingAddress.city").ofType[String].is(address.city))
      .check(jsonPath("$.result.shippingAddress.zip").ofType[String].is(address.zip))
  }
}
