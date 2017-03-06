package playground.simulations

import gatling.seeds.Seeder

// running gatling sims as an integration tests
// https://github.com/FoxComm/highlander/pull/732#discussion_r98843060
object GatlingSimulations extends App with Seeder {
  runSimulation[EvilGuestSimulationLocally]()
  runSimulation[AdminLoginSimulation]()
}
