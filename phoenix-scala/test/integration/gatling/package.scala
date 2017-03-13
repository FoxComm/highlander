import io.gatling.app.Gatling
import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.core.structure.{ChainBuilder, StructureBuilder}
import io.gatling.http.request.builder.HttpRequestBuilder

import scala.concurrent.duration._
import scala.reflect.{ClassTag, classTag}

package object gatling {

  implicit class StopOnFailure[B <: StructureBuilder[B]](val builder: B) extends AnyVal {
    def stopOnFailure =
      builder.exec {
        doIf(session ⇒ session.isFailed)(exec { session ⇒
          Console.err.println("[ERROR] Gatling ITs failed, exiting.")
          session.onExit(session)
          // TODO break tests here
          session
        })
      }
  }

  // run gatling with no reports being generated
  def runSimulation[A: ClassTag](): Int =
    Gatling.fromArgs(Array("-nr"), Some(classTag[A].runtimeClass.asInstanceOf[Class[Simulation]]))

  implicit class Stepper[B <: StructureBuilder[B]](val builder: B) extends AnyVal {
    def go(http: HttpRequestBuilder) = builder.exec(http).stopOnFailure
  }
}
