package com.foxcommerce.perf.endpoints.write.storefront

import com.foxcommerce.perf.endpoints.read.storefront.AnonCustomerEndpoint
import com.foxcommerce.common.Config
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder
import io.gatling.core.structure.ChainBuilder
import io.gatling.jsonpath.JsonPath

object CartEndpoint {

  def getCart(): HttpRequestBuilder = http("Get My Account")
    .get("/api/v1/my/cart")
    .check(status.is(200))

    def addLineItems(): ChainBuilder =
      foreach("${products}", "product") {
        exec(session ⇒ {
          val json = session("product").as[Map[String,Any]]
          val productId = json("productId")
          session.set("productId", productId)
        })
        .exec(AnonCustomerEndpoint.getProductPdpData())
        .exec(
            http("Add ${code} to line items")
            .post("/api/v1/my/cart/line-items")
            .body(StringBody("""[{"sku": "${code}", "quantity": 1}]"""))
            .check(status.is(200)))
      }

}
