import sbt._

object Dependencies {
  object versions {
    val akkaHttp = "10.0.7"
    val cats = "0.9.0"
    val circe = "0.8.0"
    val eff = "4.3.5"
    val elastic4s = "2.1.2"
    val finch = "0.14.0"
    val http4s = "0.17.0-M3"
    val monix = "2.3.0"
  }

  val core = Seq(
    "com.github.pureconfig" %% "pureconfig" % "0.7.2",
    "com.typesafe" % "config" % "1.3.1",
    "org.typelevel" %% "cats-core" % versions.cats
  )

  val es = Seq(
    "com.sksamuel.elastic4s" %% "elastic4s-core" % versions.elastic4s,
    "com.sksamuel.elastic4s" %% "elastic4s-streams" % versions.elastic4s
  )

  val json = Seq(
    "io.circe" %% "circe-core" % versions.circe,
    "io.circe" %% "circe-generic" % versions.circe,
    "io.circe" %% "circe-parser" % versions.circe
  )

  val akkaHttp = Seq(
    "com.typesafe.akka" %% "akka-http" % versions.akkaHttp,
    "de.heikoseeberger" %% 	"akka-http-circe" % "1.16.1"
  )

  val finch = Seq(
    "com.github.finagle" %% "finch-circe" % versions.finch,
    "com.github.finagle" %% "finch-core" % versions.finch,
    "com.github.finagle" %% "finch-generic" % versions.finch,
    "io.monix" %% "monix-eval" % versions.monix,
    "io.monix" %% "monix-reactive" % versions.monix
  )

  val http4s = Seq(
    "org.http4s" %% "http4s-dsl" % versions.http4s,
    "org.http4s" %% "http4s-blaze-server" % versions.http4s,
    "org.http4s" %% "http4s-circe" % versions.http4s
  )
}
