package gatling

import akka.http.scaladsl.model.Uri
import io.gatling.app.Gatling
import io.gatling.core.scenario.Simulation
import testutils.{HttpSupport, IntegrationTestBase, JWTAuth}
import testutils.apis.{PhoenixAdminApi, PhoenixMyApi, PhoenixPublicApi}
import testutils.fixtures.BakedFixtures
import testutils.fixtures.api.ApiFixtures

import scala.reflect.{ClassTag, classTag}

class GatlingIntegrationTest
    extends IntegrationTestBase
    with PhoenixPublicApi
    with PhoenixAdminApi
    with PhoenixMyApi
    with JWTAuth {

  def runSimulation[A: ClassTag](): Int =
    Gatling.fromArgs(Array(), Some(classTag[A].runtimeClass.asInstanceOf[Class[Simulation]]))

  "smoke test" - {
    "gatling should run" in {
      runSimulation[EvilGuestSimulationLocally]()
      println("yay")
    }
  }
}
