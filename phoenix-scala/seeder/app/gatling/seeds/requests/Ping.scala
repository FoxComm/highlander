package gatling.seeds.requests

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import gatling.seeds.Conf

object Ping {

  val ping =
    http("Check if phoenix is responding").get("/v1/public/ping").check(bodyString.saveAs("pong"))

  val waitForPhoenix = asLongAs(session ⇒ !session.contains("pong")) {
    doIf(session ⇒ session.isFailed)(pause(Conf.phoenixPingPause)).exec(ping)
  }
}
