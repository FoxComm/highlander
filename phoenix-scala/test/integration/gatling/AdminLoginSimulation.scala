package gatling

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import org.json4s.jackson.Serialization.{write ⇒ json}
import payloads.LoginPayload
import utils.JsonFormatters

import scala.language.postfixOps

class AdminLoginSimulation extends BaseSimulation {

  override def scn = {
    val loginPayload = LoginPayload("admin@admin.com", "password", "tenant")

    scenario("Admin must be able to log in")
      .go(
          http("do login")
            .post("/v1/public/login")
            .body(StringBody(json(loginPayload)))
            .check(status.is(200)))
      .go(http("do logout").post("/v1/public/logout").check(status.is(302)))
  }

}
