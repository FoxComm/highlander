import sbt._

object Dependencies {
  object versions {
    val cats      = "0.9.0"
    val circe     = "0.8.0"
    val elastic4s = "2.1.2"
    val finch     = "0.14.0"
  }

  val core = Seq(
    "com.github.pureconfig" %% "pureconfig" % "0.7.2",
    "com.typesafe"          % "config"      % "1.3.1",
    "org.typelevel"         %% "cats-core"  % versions.cats
  )

  val es = Seq(
    "com.fasterxml.jackson.dataformat" % "jackson-dataformat-smile" % "2.8.2",
    "com.sksamuel.elastic4s"           %% "elastic4s-core"          % versions.elastic4s
  )

  val circe = Seq(
    "io.circe" %% "circe-core"           % versions.circe,
    "io.circe" %% "circe-generic-extras" % versions.circe,
    "io.circe" %% "circe-parser"         % versions.circe
  )

  val finch = Seq(
    "com.github.finagle" %% "finch-circe"   % versions.finch,
    "com.github.finagle" %% "finch-core"    % versions.finch,
    "com.github.finagle" %% "finch-generic" % versions.finch
  )

  val jwt = "com.pauldijou" %% "jwt-core" % "0.12.1"

  object test {
    def core =
      Seq(
        "org.scalatest" %% "scalatest" % "3.0.3"
      ).map(_ % "test")
  }
}
