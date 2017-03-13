import io.gatling.core.Predef._
import io.gatling.core.structure.{ChainBuilder, StructureBuilder}
import io.gatling.http.request.builder.HttpRequestBuilder

import scala.concurrent.duration._

package object gatling {

  implicit class StopOnFailure[B <: StructureBuilder[B]](val builder: B) extends AnyVal {
    def stopOnFailure =
      builder.exec {
        doIf(session ⇒ session.isFailed)(exec { session ⇒
          Console.err.println("[ERROR] Gatling ITs failed, exiting.")
          session.onExit(session)
          System.exit(1)
          session
        })
      }
  }

  implicit class Stepper[B <: StructureBuilder[B]](val builder: B) extends AnyVal {
    def go(http: HttpRequestBuilder) = builder.exec(http).stopOnFailure
  }
}
