import io.gatling.app.Gatling
import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.core.structure.StructureBuilder
import io.gatling.http.request.builder.HttpRequestBuilder

import scala.reflect.{classTag, ClassTag}

package object gatling {

  implicit class StopOnFailure[B <: StructureBuilder[B]](val builder: B) extends AnyVal {
    def stopOnFailure: B =
      builder.exec {
        doIf(session ⇒ session.isFailed)(exec { session ⇒
          println("Gatling test failed!")
          System.exit(1)
          session.exit()
          session
        })
      }
  }

  // run gatling with no reports being generated
  def runSimulation[A: ClassTag](): Int =
    Gatling.fromArgs(Array("-nr"), Some(classTag[A].runtimeClass.asInstanceOf[Class[Simulation]]))

  implicit class Stepper[B <: StructureBuilder[B]](val builder: B) extends AnyVal {
    def go(http: HttpRequestBuilder): B = builder.exec(http).stopOnFailure
  }

}
