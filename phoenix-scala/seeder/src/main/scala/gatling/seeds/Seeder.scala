package gatling.seeds

import scala.reflect.{ClassTag, classTag}

import io.gatling.app.Gatling
import io.gatling.core.scenario.Simulation
import gatling.seeds.simulations._

trait Seeder {

  def pingPhoenix(): Unit = {
    val pingExitCode = runSimulation[PhoenixPingSimulation]()
    if (pingExitCode != 0) {
      println(s"Phoenix did not respond in ${Conf.phoenixStartupTimeout}, exiting now!")
      System.exit(1)
    }
  }

  /**
    * Run gatling simulation with no args
    * @tparam A Simulation class
    * @return Simulation exit code
    */
  def runSimulation[A: ClassTag](): Int =
    Gatling.fromArgs(Array(), Some(classTag[A].runtimeClass.asInstanceOf[Class[Simulation]]))
}

object OneshotSeeds extends App with Seeder {
  pingPhoenix()
  runSimulation[OneshotSeedsSimulation]()
}

object ContinuousSeeds extends App with Seeder {
  pingPhoenix()
  runSimulation[CustomerActivitySimulation]()
}
