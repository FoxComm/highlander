package gatling

import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.core.structure.StructureBuilder
import io.gatling.jdbc.Predef._
import gatling.seeds.Conf._

package object seeds {

  def dbFeeder(sql: String) = jdbcFeeder(dbUrl, dbUser, dbPassword, sql)

  implicit class StopOnFailure[B <: StructureBuilder[B]](val builder: B) extends AnyVal {
    def stopOnFailure =
      builder.exec {
        doIf(session ⇒ session.isFailed)(exec { session ⇒
          Console.err.println("[ERROR] Seeds failed, exiting.")
          session.onExit(session)
          System.exit(1)
          session
        })
      }
  }

  implicit class DefaultPause[B <: StructureBuilder[B]](val builder: B) extends AnyVal {
    def doPause = builder.pause(100.milliseconds, 1.second)
  }
}
