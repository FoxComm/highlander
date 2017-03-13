package gatling

import akka.http.scaladsl.model.Uri
import io.gatling.core.scenario.Simulation
import io.gatling.core.structure.ScenarioBuilder
import testutils.HttpSupport
import utils.JsonFormatters
import io.gatling.http.Predef._
import io.gatling.core.Predef._

abstract class BaseSimulation extends Simulation {

  lazy val baseUrl = {
    val host = HttpSupport.serverBinding.localAddress.getHostString
    val port = HttpSupport.serverBinding.localAddress.getPort
    Uri(s"http://$host:$port")
  }

  val defaultAssertion = global.failedRequests.count.is(0)
  implicit val formats = JsonFormatters.phoenixFormats
  lazy val httpConf = http
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .disableWarmUp
    .disableFollowRedirect
    .baseURL(baseUrl.toString())

  def scn: ScenarioBuilder

  setUp(scn.inject(atOnceUsers(1))).protocols(httpConf)

}
