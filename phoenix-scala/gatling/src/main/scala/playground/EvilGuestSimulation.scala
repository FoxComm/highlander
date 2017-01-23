package playground

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import org.json4s.jackson.Serialization.{write ⇒ json}
import payloads.CustomerPayloads.CreateCustomerPayload
import payloads.LoginPayload
import seeds.Seeder
import utils.JsonFormatters
import helpers._
import scala.language.postfixOps
import scala.util.Random

// test scenarios for https://github.com/FoxComm/highlander/pull/732
class EvilGuestSimulationLocally extends Scenarios {
  setup("http://localhost:9090") // todo get from config?
}

trait Scenarios extends Simulation {
  implicit val formats = JsonFormatters.phoenixFormats
  val connection       = http.contentTypeHeader("application/json;charset=UTF-8").disableFollowRedirect

  private val userEmail = "guest%d@guest.com".format(Random.nextInt(1000))
  val loginPayload      = LoginPayload(userEmail, "123", "merchant")
  val customerPayload = CreateCustomerPayload(email = userEmail,
                                              name = Some("guest"),
                                              password = Some(loginPayload.password))

  val scn = scenario("EvilGuestSimulation")
    .go(http("user should not exist")
          .post("/v1/public/login")
          .body(StringBody(json(loginPayload)))
          .check(status.is(400)))
    .go(http("create a new user")
          .post("/v1/public/registrations/new")
          .body(StringBody(json(customerPayload))))
    .go(http("see my cart").get("/v1/my/cart"))
    .go(http("do logout").post("/v1/public/logout").check(status.is(302)))
    .go(http("do login")
          .post("/v1/public/login")
          .body(StringBody(json(loginPayload)))
          .check(status.is(200)))
    .go(http("do logout").post("/v1/public/logout").check(status.is(302)))
    .go(http("see my cart").get("/v1/my/cart"))
    .go(http("see my acc").get("/v1/my/account"))
    .go(http("patch guest account")
          .patch("/v1/my/account")
          .body(StringBody(s"""{"email":"$userEmail"}""")))
    .exec(flushCookieJar) // no JWT token goes further
    .go(http("second login")
          .post("/v1/public/login")
          .body(StringBody(json(loginPayload)))
          .check(status.is(200)))

  def setup(url: String) = {
    val conn = connection.baseURL(url)
    setUp(scn.inject(atOnceUsers(1))).protocols(conn)
  }
}
