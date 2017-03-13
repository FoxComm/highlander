package gatling

import io.gatling.app.Gatling
import io.gatling.core.scenario.Simulation
import testutils.apis.{PhoenixAdminApi, PhoenixMyApi, PhoenixPublicApi}
import testutils.{IntegrationTestBase, JWTAuth}

import scala.reflect.{ClassTag, classTag}

class GatlingIntegrationTest
    extends IntegrationTestBase
    with PhoenixPublicApi
    with PhoenixAdminApi
    with PhoenixMyApi
    with JWTAuth {

  "smoke test" - {
    "gatling should run" in {
      runSimulation[EvilGuestSimulation]()
      println("yay")
    }
  }
}
