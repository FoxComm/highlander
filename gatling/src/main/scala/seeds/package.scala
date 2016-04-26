import io.gatling.core.Predef._
import io.gatling.core.structure.StructureBuilder
import utils.JsonFormatters

package object seeds {

  implicit val formats = JsonFormatters.phoenixFormats

  implicit class StopOnFailure[B <: StructureBuilder[B]](val builder: B) extends AnyVal {
    def stopOnFailure = builder.exec(doIf(session ⇒ session.isFailed)(exec { session ⇒
      Console.err.println("Seeds failed, exiting.")
      System.exit(1)
      session
    }))
  }
}
