import sbt._

object Dependencies {
  object versions {
    val cats          = "0.9.0"
    val circe         = "0.8.0"
    val elasticsearch = "2.1.2"
    val finch         = "0.14.0"
    val monix         = "2.3.0"
  }

  val core = Seq(
    "com.github.pureconfig" %% "pureconfig" % "0.7.2",
    "com.typesafe"          % "config"      % "1.3.1",
    "org.typelevel"         %% "cats-core"  % versions.cats
  )

  val es = Seq(
    "org.elasticsearch" % "elasticsearch" % versions.elasticsearch
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

  val jwt = Seq(
    "com.pauldijou" %% "jwt-core" % "0.12.1"
  )

  val monix = Seq(
    "io.monix" %% "monix-cats" % versions.monix,
    "io.monix" %% "monix-eval" % versions.monix
  )

  object test {
    def core =
      Seq(
        "org.scalatest" %% "scalatest" % "3.0.3"
      ).map(_ % "test")
  }
}
