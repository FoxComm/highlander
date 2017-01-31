package playground

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import seeds.Seeder

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

// test scenarios for https://github.com/FoxComm/highlander/pull/732
object RunEvilGuestSimulation extends App with Seeder {
  runSimulation[EvilGuestSimulationConfirmed]()
//  runSimulation[EvilGuestSimulationNOTConfirmed]()
}

class EvilGuestSimulationConfirmed extends Scenarios {
  setup("https://appliance-10-240-0-5.foxcommerce.com/api/")
  //  or ("https://stage-tpg.foxcommerce.com/api/")
}

class EvilGuestSimulationNOTConfirmed extends Scenarios {
  setup("http://localhost:9090")
}

trait Scenarios extends Simulation {

  val connection = http.contentTypeHeader("application/json;charset=UTF-8").disableFollowRedirect

  private val userEmail = "guest%d@guest.com".format(Random.nextInt(1000))
  private val loginPayload = StringBody(
      s"""{"email":"$userEmail","password":"123","org":"merchant"}""")

  val scn = scenario("EvilGuestSimulation")
    .exec(http("user should not exist")
          .post("/v1/public/login")
          .body(loginPayload)
          .check(status.is(400)))
    .exec(http("create a new user")
          .post("/v1/public/registrations/new")
          .body(StringBody(s"""{"email":"$userEmail","name":"guest","password":"123"}""")))
    .exec(http("see my cart").get("/v1/my/cart"))
    .exec(http("do logout").post("/v1/public/logout").check(status.is(302)))
    .exec(http("do login").post("/v1/public/login").body(loginPayload).check(status.is(200)))
    .exec(http("do logout").post("/v1/public/logout").check(status.is(302)))
    .exec(http("see my cart").get("/v1/my/cart"))
    .exec(http("see my acc").get("/v1/my/account"))
    .exec(http("patch guest account")
          .patch("/v1/my/account")
          .body(StringBody(s"""{"email":"$userEmail"}""")))
    .exec(flushCookieJar) // no JWT token goes further
    .exec(http("second login").post("/v1/public/login").body(loginPayload).check(status.is(200)))

  def setup(url: String) = {
    val conn = connection.baseURL(url)
    setUp(scn.inject(atOnceUsers(1))).protocols(conn)
  }
}
