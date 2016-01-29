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

  def addShippingAddress(order: OrderPayload): HttpRequestBuilder = {
    http("Add Order Shipping Address")
      .post("/v1/orders/${orderRefNum}/shipping-address")
      .basicAuth("${email}", "${password}")
      .body(StringBody(Utils.addressPayloadBody(order.shippingAddress)))
      .check(status.is(200))
      .check(jsonPath("$.result.shippingAddress.name").ofType[String].is(order.shippingAddress.name))
      .check(jsonPath("$.result.shippingAddress.region.id").ofType[Long].is(order.shippingAddress.regionId))
      .check(jsonPath("$.result.shippingAddress.address1").ofType[String].is(order.shippingAddress.address1))
      .check(jsonPath("$.result.shippingAddress.address2").ofType[String].is(order.shippingAddress.address2))
      .check(jsonPath("$.result.shippingAddress.city").ofType[String].is(order.shippingAddress.city))
      .check(jsonPath("$.result.shippingAddress.zip").ofType[String].is(order.shippingAddress.zip))
  }

  def updateShippingAddress(order: OrderPayload): HttpRequestBuilder = {
    http("Update Order Shipping Address")
      .patch("/v1/orders/${orderRefNum}/shipping-address")
      .basicAuth("${email}", "${password}")
      .body(StringBody(Utils.addressPayloadBody(order.shippingAddress)))
      .check(status.is(200))
      .check(jsonPath("$.result.shippingAddress.name").ofType[String].is(order.shippingAddress.name))
      .check(jsonPath("$.result.shippingAddress.region.id").ofType[Long].is(order.shippingAddress.regionId))
      .check(jsonPath("$.result.shippingAddress.address1").ofType[String].is(order.shippingAddress.address1))
      .check(jsonPath("$.result.shippingAddress.address2").ofType[String].is(order.shippingAddress.address2))
      .check(jsonPath("$.result.shippingAddress.city").ofType[String].is(order.shippingAddress.city))
      .check(jsonPath("$.result.shippingAddress.zip").ofType[String].is(order.shippingAddress.zip))
  }
}
