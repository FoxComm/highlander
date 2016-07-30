package com.foxcommerce.common

import com.typesafe.config.ConfigFactory

import io.gatling.core.Predef._
import io.gatling.http.Predef._

final case class Config(environment: String, apiUrl: String, userAgent: String, usersCount: Int,
  greenRiverPause: Int) {

  val defaultAssertion        = global.failedRequests.count.is(0)
  val defaultInjectionProfile = atOnceUsers(usersCount)

  val httpConf = http
      .baseURL(apiUrl)
      .acceptHeader("application/json")
      .contentTypeHeader("application/json")
      .userAgentHeader(userAgent)
      .disableWarmUp

  def before(): Unit = {
    Console.println(s"Starting simulation in $environment environment")
    Console.println(s"API URL: $apiUrl")
  }
}

object Config {
  // Can override this in config later
  val jwtCookie          = "JWT"
  val defaultEnvironment = "vagrant"
  val defaultUsersCount  = 1
  val defaultPause       = 15

  def load(): Config = {
    val env   = sys.props.getOrElse("env", defaultEnvironment)
    val users = sys.props.getOrElse("users", defaultUsersCount).toString.toInt
    val pause = sys.props.getOrElse("pause", defaultPause).toString.toInt
    val conf  = ConfigFactory.load()

    Config(
      environment     = env,
      apiUrl          = conf.getString(s"$env.apiUrl"),
      userAgent       = conf.getString(s"$env.userAgent"),
      usersCount      = users,
      greenRiverPause = pause
    )
  }
}
