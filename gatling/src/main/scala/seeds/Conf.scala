package seeds

import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import utils.Money.Currency._

object Conf {

  val defaultAssertion = global.failedRequests.count.is(0)

  val httpConf = http
    .baseURL("http://localhost:9090")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .disableWarmUp

  val phoenixPingPause = 10.seconds
  val phoenixStartupTimeout = 1.minute

  val contexts = Seq(("default", USD), ("ru", RUB))

}
