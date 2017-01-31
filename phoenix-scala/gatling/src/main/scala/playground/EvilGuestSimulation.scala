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
object RunEvilGuestSimulation extends App with Seeder {
  runSimulation[EvilGuestSimulationOnVm]()
//  runSimulation[EvilGuestSimulationLocally]()
}

class EvilGuestSimulationOnVm extends Scenarios {
  setup("https://appliance-10-240-0-5.foxcommerce.com/api/")
  //  or ("https://stage-tpg.foxcommerce.com/api/")
}

class EvilGuestSimulationLocally extends Scenarios {
  setup("http://localhost:9090")
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
    .step(http("user should not exist")
          .post("/v1/public/login")
          .body(StringBody(json {
        loginPayload
      }))
          .check(status.is(400)))
    .step(http("create a new user")
          .post("/v1/public/registrations/new")
          .body(StringBody(json {
        customerPayload
      })))
    .step(http("see my cart").get("/v1/my/cart"))
    .step(http("do logout").post("/v1/public/logout").check(status.is(302)))
    .step(http("do login")
          .post("/v1/public/login")
          .body(StringBody(json {
        loginPayload
      }))
          .check(status.is(200)))
    .step(http("do logout").post("/v1/public/logout").check(status.is(302)))
    .step(http("see my cart").get("/v1/my/cart"))
    .step(http("see my acc").get("/v1/my/account"))
    .step(http("patch guest account")
          .patch("/v1/my/account")
          .body(StringBody(s"""{"email":"$userEmail"}""")))
    .exec(flushCookieJar) // no JWT token goes further
    .step(http("second login")
          .post("/v1/public/login")
          .body(StringBody(json {
        loginPayload
      }))
          .check(status.is(200)))

  def setup(url: String) = {
    val conn = connection.baseURL(url)
    setUp(scn.inject(atOnceUsers(1))).protocols(conn)
  }
}
