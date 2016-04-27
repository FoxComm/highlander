import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.core.structure.StructureBuilder
import utils.JsonFormatters

package object seeds {

  implicit val formats = JsonFormatters.phoenixFormats

  implicit class StopOnFailure[B <: StructureBuilder[B]](val builder: B) extends AnyVal {
    def stopOnFailure = builder.exec(doIf(session ⇒ session.isFailed)(exec { session ⇒
      Console.err.println("[ERROR] Seeds failed, exiting.")
      session.terminate
      System.exit(1)
      session
    }))
  }

  implicit class DefaultPause[B <: StructureBuilder[B]](val builder: B) extends AnyVal {
    def doPause = builder.pause(100.milliseconds, 1.second)
  }
}
