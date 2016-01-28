package com.foxcommerce.common

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder

object ActivityConnection {

  val typeName = "activity_connections"

  def assert(conf: Config, activityType: String): HttpRequestBuilder = {
    http("Assert Activity Connection Presence")
      .get(s"${conf.elasticUrl}/${conf.indexName}/$typeName/_search?sort=id:desc")
      .check(status.is(200))
      .check(jsonPath("$.hits.hits[0].id").ofType[Long].saveAs("activityConnectionId"))
      .check(jsonPath("$.hits.hits[0].activity.kind").ofType[String].is(activityType))
  }

  def delete(conf: Config): HttpRequestBuilder = {
    http("Delete Activity Connection")
      .get(s"${conf.elasticUrl}/${conf.indexName}/$typeName" ++ "/${activityConnectionId}")
      .check(status.is(200))
      .check(jsonPath("$.found").ofType[Boolean].is(true))
  }
}