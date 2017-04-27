package gatling.seeds.requests

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder
import org.json4s.jackson.Serialization.{write ⇒ json}
import payloads.LoginPayload
import utils.JsonFormatters

object Auth {

  implicit val formats = JsonFormatters.phoenixFormats

  val loginAsAdmin = http("Login as Admin")
    .post("/v1/public/login")
    .body(StringBody(json(LoginPayload(email = "${adminEmail}",
                                       password = "${adminPassword}",
                                       org = "${adminOrg}"))))
    .check(header("JWT").saveAs("jwtTokenAdmin"))

  val loginAsRandomAdmin = feed(csv("data/store_admins.csv").random).exec(loginAsAdmin)

  implicit class RequireAuth(val httpBuilder: HttpRequestBuilder) extends AnyVal {
    def requireAdminAuth =
      httpBuilder.header("Authorization", "${jwtTokenAdmin}")
  }
}
