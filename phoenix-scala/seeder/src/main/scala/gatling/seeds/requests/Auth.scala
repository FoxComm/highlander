package gatling.seeds.requests

import io.circe.jackson.syntax._
import io.circe.syntax._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder
import payloads.LoginPayload

object Auth {
  val loginAsAdmin = http("Login as Admin")
    .post("/v1/public/login")
    .body(StringBody(LoginPayload(email = "${adminEmail}",
                                       password = "${adminPassword}",
                                       org = "${adminOrg}").asJson.jacksonPrint))
    .check(header("JWT").saveAs("jwtTokenAdmin"))

  val loginAsRandomAdmin = feed(csv("data/store_admins.csv").random).exec(loginAsAdmin)

  implicit class RequireAuth(val httpBuilder: HttpRequestBuilder) extends AnyVal {
    def requireAdminAuth =
      httpBuilder.header("Authorization", "${jwtTokenAdmin}")
  }
}
