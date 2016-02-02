package com.foxcommerce.common

import com.typesafe.config.ConfigFactory

import io.gatling.core.Predef._
import io.gatling.http.Predef._

final case class Config(environment: String, phoenixUrl: String, elasticUrl: String, indexName: String,
  userAgent: String, usersCount: Int, greenRiverPause: Int) {

  val defaultAssertion        = global.failedRequests.count.is(0)
  val defaultInjectionProfile = atOnceUsers(usersCount)

  val httpConf = http
      .baseURL(phoenixUrl)
      .acceptHeader("application/json")
      .contentTypeHeader("application/json")
      .userAgentHeader(userAgent)
      .disableWarmUp

  def before(): Unit = {
    Console.println(s"Starting simulation in $environment environment")
    Console.println(s"Phoenix URL: $phoenixUrl")
    Console.println(s"Elasticsearch URL: $elasticUrl")
  }
}

object Config {
  val defaultEnvironment = "vagrant"
  val defaultUsersCount  = 1
  val defaultPause       = 7

  def load(): Config = {
    val env   = sys.props.getOrElse("env", defaultEnvironment)
    val users = sys.props.getOrElse("users", defaultUsersCount).toString.toInt
    val pause = sys.props.getOrElse("pause", defaultPause).toString.toInt
    val conf  = ConfigFactory.load()

    Config(
      environment     = env,
      phoenixUrl      = conf.getString(s"$env.phoenixUrl"),
      elasticUrl      = conf.getString(s"$env.elasticUrl"),
      indexName       = conf.getString(s"$env.elasticIndex"),
      userAgent       = conf.getString(s"$env.userAgent"),
      usersCount      = users,
      greenRiverPause = pause
    )
  }
}
