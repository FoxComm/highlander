import gatling.{AdminLoginSimulation, EvilGuestSimulation, runSimulation}
import testutils.{IntegrationTestBase, JWTAuth}

class GatlingSimulationsIntegrationTest extends IntegrationTestBase with JWTAuth {

  "Gatling tests must be green in" - {
    "Guest checkout must not break user account with the same email" in {
      runSimulation[EvilGuestSimulation]()
    }

    "Admin must be able to log in" in new StoreAdmin_Seed {
      runSimulation[AdminLoginSimulation]()
    }
  }
}
