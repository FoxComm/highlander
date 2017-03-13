package gatling

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import org.json4s.jackson.Serialization.{write ⇒ json}
import payloads.LoginPayload
import utils.JsonFormatters

import scala.language.postfixOps

class AdminLoginSimulation extends Simulation {

  implicit val formats = JsonFormatters.phoenixFormats
  val connection       = http.contentTypeHeader("application/json;charset=UTF-8").disableFollowRedirect

  val loginPayload = LoginPayload("admin@admin.com", "password", "tenant")

  val scn = scenario("AdminLoginSimulation")
    .go(
        http("do login")
          .post("/v1/public/login")
          .body(StringBody(json(loginPayload)))
          .check(status.is(200)))
    .go(http("do logout").post("/v1/public/logout").check(status.is(302)))

  def setup(url: String) = {
    val conn = connection.baseURL(url)
    setUp(scn.inject(atOnceUsers(1))).protocols(conn)
  }

  setup("http://localhost:9090")
}
