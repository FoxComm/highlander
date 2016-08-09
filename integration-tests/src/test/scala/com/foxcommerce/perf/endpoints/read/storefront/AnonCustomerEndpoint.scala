
package com.foxcommerce.perf.endpoints.read.storefront

import com.foxcommerce.common.Config
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder
import io.gatling.core.structure.ChainBuilder
import io.gatling.jsonpath.JsonPath

object AnonCustomerEndpoint {

  def frontpage(): HttpRequestBuilder = http("Access Frontpage")
    .get("/")
    .check(status.is(200))

  def getProductList(): HttpRequestBuilder = http("Access Product List")
    .get("/api/search/public/products_catalog_view/_search?")
    .check(status.is(200))
    .check(jsonPath("$.result[*]").ofType[Map[String, Any]].findAll.saveAs("products"))

  def getProductPdp(): HttpRequestBuilder = http("Access PDP For ${productId}")
    .get("/products/${productId}")
    .check(status.is(200))

  def getProductPdpData(): HttpRequestBuilder = http("Access PDP Data For ${productId}")
    .get("/api/v1/public/products/${productId}")
    .check(status.is(200))
    .check(jsonPath("$.skus[0].attributes.code.v").ofType[String].saveAs("code"))

  def getProductPdps(): ChainBuilder = 
    foreach("${products}", "product") {
      exec(session ⇒ {
        val json = session("product").as[Map[String,Any]]
        val productId = json("productId")
        session.set("productId", productId)
      })
      .exec(getProductPdp())
      .exec(getProductPdpData())
    }
}
