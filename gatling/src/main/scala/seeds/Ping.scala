package seeds

import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._

object Ping {

  val ping = http("Check if phoenix is responding")
    .get("/v1/public/ping")
    .check(status.is(200), bodyString.saveAs("pong"))

  private val phoenixPingPause = 10.seconds

  val waitForPhoenix =
    asLongAs(session ⇒ !session.contains("pong")) {
       doIf(session ⇒ session.isFailed)(pause(phoenixPingPause))
      .exec(ping)
    }
}
