package seeds

import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._

object Conf {

  val defaultAssertion = global.failedRequests.count.is(0)

  val httpConf = http
    .baseURL("http://localhost:9090")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .disableWarmUp

  val phoenixStartupTimeout = 1.minute
}
