package com.foxcommerce

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class BasicSimulation extends Simulation {

  // TBD: Move this to object, read configuration based on environment
  val httpConf = http
    .baseURL("http://foxcommerce.com")
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    .doNotTrackHeader("1")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .acceptEncodingHeader("gzip, deflate")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")

  val usersFeeder = jsonFile("data/users.json").random

  // TODO: Move executions to objects
  val mainScenario = scenario("Main Page Test")
    .feed(usersFeeder)
    .exec(
      http("Request by user ${name}")
        .get("/")
        .check(status.is(200))
        .check(css("title").is("FoxCommerce"))
    )

  setUp(
    mainScenario.inject(atOnceUsers(5)).protocols(httpConf)
  )
}
