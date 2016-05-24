package seeds

import io.gatling.app.Gatling
import io.gatling.core.scenario.Simulation
import seeds.simulations._

trait Seeder {

  def pingPhoenix(): Unit = {
    val pingExitCode = Gatling.fromArgs(
        Array(), Some(classOf[PhoenixPingSimulation].asInstanceOf[Class[Simulation]]))
    if (pingExitCode != 0) {
      println(s"Phoenix did not respond in ${Conf.phoenixStartupTimeout}, exiting now!")
      System.exit(1)
    }
  }
}

object OneshotSeeds extends App with Seeder {
  pingPhoenix()
  Gatling.fromArgs(Array(), Some(classOf[OneshotSeedsSimulation].asInstanceOf[Class[Simulation]]))
}

object ContinuousSeeds extends App with Seeder {
  pingPhoenix()
  Gatling.fromArgs(
      Array(), Some(classOf[CustomerActivitySimulation].asInstanceOf[Class[Simulation]]))
}
