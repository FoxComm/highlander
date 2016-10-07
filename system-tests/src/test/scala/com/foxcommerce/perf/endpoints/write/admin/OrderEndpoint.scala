package com.foxcommerce.perf.endpoints.write.admin

import com.foxcommerce.common._
import com.foxcommerce.fixtures._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder

object OrderEndpoint {

  def create(order: OrderFixture): HttpRequestBuilder = http("Create Order")
    .post("/api/v1/orders")
    .body(StringBody("""{"customerId": ${customerId}}"""))
    .check(status.is(200))
    .check(jsonPath("$.referenceNumber").ofType[String].saveAs("orderRefNum"))

  def cancel(): HttpRequestBuilder = http("Cancel Order")
    .patch("/api/v1/orders/${orderRefNum}")
    .body(StringBody("""{"state": "canceled"}"""))
    .check(status.is(200))

  def addShippingAddress(order: OrderFixture): HttpRequestBuilder = {
    http("Add Order Shipping Address")
      .post("/api/v1/orders/${orderRefNum}/shipping-address")
      .body(StringBody(Utils.addressPayloadBody(order.shippingAddress)))
      .check(status.is(200))
  }

  def updateShippingAddress(order: OrderFixture): HttpRequestBuilder = http("Update Order Shipping Address")
    .patch("/api/v1/orders/${orderRefNum}/shipping-address")
    .body(StringBody(Utils.addressPayloadBody(order.shippingAddress)))
    .check(status.is(200))

  def assign(storeAdminId: Int): HttpRequestBuilder = http("Assign Store Admin To Order")
    .post("/api/v1/orders/${orderRefNum}/assignees")
    .body(StringBody("""{"assignees": [%d]}""".format(storeAdminId)))
    .check(status.is(200))
}
