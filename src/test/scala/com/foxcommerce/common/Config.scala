package com.foxcommerce.common

import com.typesafe.config.ConfigFactory

import io.gatling.core.Predef._
import io.gatling.http.Predef._

final case class Config(environment: String, phoenixUrl: String, elasticUrl: String, userAgent: String) {
  val defaultAssertion = global.failedRequests.count.is(0)
  val greenRiverPause = 5
  val usersCount = 1

  val httpConf = http
      .baseURL(phoenixUrl)
      .acceptHeader("application/json")
      .contentTypeHeader("application/json")
      .userAgentHeader(userAgent)

  def before(): Unit = {
    Console.println(s"Starting simulation in $environment environment")
    Console.println(s"Phoenix URL: $phoenixUrl")
    Console.println(s"Elasticsearch URL: $elasticUrl")
  }
}

object Config {
  val defaultEnvironment = "vagrant"

  def load(): Config = {
    val env = sys.props.getOrElse("env", defaultEnvironment)
    val conf = ConfigFactory.load()

    Config(
      environment = env,
      phoenixUrl  = conf.getString(s"$env.phoenixUrl"),
      elasticUrl  = conf.getString(s"$env.elasticUrl"),
      userAgent   = conf.getString(s"$env.userAgent")
    )
  }
}