package seeds.playground

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import seeds.Seeder

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

object RunEvilGuestSimulation extends App with Seeder {
  runSimulation[EvilGuestSimulation]()
}

// may we keep this simulation somewhere? @_@
class EvilGuestSimulation extends Simulation {

  val httpProtocol = http
//    .baseURL("http://localhost:9090")
//    .baseURL("https://stage-tpg.foxcommerce.com/api/")
    .baseURL("https://appliance-10-240-0-5.foxcommerce.com/api/")
    .contentTypeHeader("application/json;charset=UTF-8")
    .disableFollowRedirect

  val userEmail = "guest%d@guest.com".format(Random.nextInt(1000))
  private val loginPayload = StringBody(
      "{\"email\":\"%s\",\"password\":\"123\",\"org\":\"merchant\"}".format(userEmail))

  val scn = scenario("EvilGuestSimulation")
    .exec(http("request_18").post("/v1/public/login").body(loginPayload).check(status.is(400))) // not having acc beforehand
    .exec(http("request_19")
          .post("/v1/public/registrations/new")
          .body(StringBody(
                  "{\"email\":\"%s\",\"name\":\"guest\",\"password\":\"123\"}".format(userEmail)))
          .check(header("JWT").saveAs("token")))
    .exec(http("request_20").get("/v1/my/cart"))
    .exec(http("request_22").post("/v1/public/logout").check(status.is(302)))
    .exec(http("request_18").post("/v1/public/login").body(loginPayload).check(status.is(200)))
    .exec(http("request_22").post("/v1/public/logout").check(status.is(302)))
    .exec(http("request_23").get("/v1/my/cart"))
    .exec(http("request_35").get("/v1/my/account"))
    .exec(http("request_38")
          .patch("/v1/my/account")
          .check(header("JWT").saveAs("token"))
          .body(StringBody("{\"email\":\"%s\"}".format(userEmail))))
    .exec(flushCookieJar) // no JWT token goes further
    .exec(http("request_41")
          .post("/v1/public/login")
          .body(loginPayload)
          .check(status.is(400))
          .check(bodyString.is("{\"errors\":[\"Invalid credentials\"]}")))

  setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)
}
