package gatling

import io.gatling.app.Gatling
import io.gatling.core.scenario.Simulation

import scala.reflect.{ClassTag, classTag}

// running gatling sims as an integration tests
// https://github.com/FoxComm/highlander/pull/732#discussion_r98843060
object GatlingSimulations extends App with Runner {
  runSimulation[EvilGuestSimulationLocally]()
//  runSimulation[AdminLoginSimulation]()
}

trait Runner {
  def runSimulation[A: ClassTag](): Int =
    Gatling.fromArgs(Array(), Some(classTag[A].runtimeClass.asInstanceOf[Class[Simulation]]))
}
