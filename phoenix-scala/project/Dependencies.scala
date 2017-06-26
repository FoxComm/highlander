import sbt._

object Versions {
  val scala  = "2.11.11-bin-typelevel-4"
  val slick  = "3.2.0"
  val json4s = "3.4.0"
  val akka   = "2.5.2"

  object AkkaHttp {
    // should be updated all together in sync
    val http   = "10.0.7"
    val sse    = "3.0.0"
    val json4s = "1.15.0"
  }

  val slickPg    = "0.15.0"
  val gatling    = "2.2.1"
  val dispatch   = "0.11.3"
  val fasterxml  = "2.8.2"
  val elastic4s  = "2.3.0"
  val scalatest  = "3.0.1"
  val scalacheck = "1.13.4"
}

object Dependencies {

  lazy val baseDependencies: Seq[ModuleID] = cats ++ shapeless ++ db ++ slick ++ json4s ++ logging

  val akka = Seq(
    "com.typesafe.akka" %% "akka-slf4j"     % Versions.akka,
    "com.typesafe.akka" %% "akka-actor"     % Versions.akka,
    "com.typesafe.akka" %% "akka-stream"    % Versions.akka,
    "com.typesafe.akka" %% "akka-http-core" % Versions.AkkaHttp.http,
    ("de.heikoseeberger" %% "akka-sse" % Versions.AkkaHttp.sse).exclude("com.typesafe.akka", "akka-http")
  )

  val slick = Seq(
    "com.typesafe.slick" %% "slick"          % Versions.slick,
    "com.typesafe.slick" %% "slick-hikaricp" % Versions.slick
  )

  val fasterxml = Seq(
    "com.fasterxml.jackson.dataformat" % "jackson-dataformat-smile" % Versions.fasterxml
  )

  val json4s = Seq(
    "org.json4s" %% "json4s-core"    % Versions.json4s,
    "org.json4s" %% "json4s-jackson" % Versions.json4s,
    "org.json4s" %% "json4s-ext"     % Versions.json4s,
    ("de.heikoseeberger" %% "akka-http-json4s" % Versions.AkkaHttp.json4s)
      .exclude("com.typesafe.akka", "akka-http")
  )

  val gatling = Seq(
    "io.gatling"            % "gatling-app"               % Versions.gatling,
    "io.gatling.highcharts" % "gatling-charts-highcharts" % Versions.gatling
  )

  val db = Seq(
    "com.github.tminglei"   %% "slick-pg"        % Versions.slickPg,
    "com.github.tminglei"   %% "slick-pg_json4s" % Versions.slickPg,
    "com.zaxxer"            % "HikariCP"         % "2.6.1",
    "org.postgresql"        % "postgresql"       % "42.1.1",
    "org.flywaydb"          % "flyway-core"      % "4.2.0",
    "com.wix"               %% "accord-core"     % "0.5", // Validation
    "io.backchat.inflector" %% "scala-inflector" % "1.3.5", // used only for singularizing table names in error messages…
    ("org.joda" % "joda-money" % "0.11").exclude("org.joda", "joda-time"),
    "com.github.mauricio" %% "postgresql-async" % "0.2.20"
  )

  val apis = Seq(
    "com.sksamuel.elastic4s" %% "elastic4s-core" % Versions.elastic4s,
    "com.amazonaws"          % "aws-java-sdk"    % "1.11.15",
    "com.stripe"             % "stripe-java"     % "4.0.0"
  )

  val logging = Seq(
    "ch.qos.logback"             % "logback-classic" % "1.1.7",
    "com.typesafe.scala-logging" %% "scala-logging"  % "3.4.0",
    "com.lihaoyi"                %% "sourcecode"     % "0.1.1",
    "org.slf4j"                  % "slf4j-api"       % "1.7.21"
  )

  val test = Seq(
    "org.scalatest"     %% "scalatest"           % Versions.scalatest,
    "org.scalacheck"    %% "scalacheck"          % Versions.scalacheck,
    "org.mockito"       % "mockito-core"         % "2.1.0-beta.125",
    "com.typesafe.akka" %% "akka-testkit"        % Versions.akka,
    "com.typesafe.akka" %% "akka-stream-testkit" % Versions.akka
  ).map { testDep ⇒
    testDep % "test,it,et"
  }

  val http = Seq(
    "net.databinder.dispatch" %% "dispatch-core"          % Versions.dispatch,
    "net.databinder.dispatch" %% "dispatch-json4s-native" % Versions.dispatch
  )

  val auth = Seq(
    "org.bitbucket.b_c" % "jose4j" % "0.5.1",
    "com.lambdaworks"   % "scrypt" % "1.4.0"
  )

  val misc = Seq(
    "com.github.scopt"       %% "scopt"             % "3.5.0", // CLI args
    "com.pellucid"           %% "sealerate"         % "0.0.3",
    "it.justwrote"           %% "scala-faker"       % "0.3",
    "org.conbere"            % "markov_2.10"        % "0.2.0",
    "com.github.tototoshi"   %% "scala-csv"         % "1.3.3",
    "com.github.melrief"     %% "pureconfig"        % "0.5.1",
    "com.sksamuel.elastic4s" %% "elastic4s-streams" % Versions.elastic4s
  )

  val cats = Seq(
    "org.typelevel" %% "cats" % "0.9.0"
  )

  val shapeless = Seq(
    "com.chuusai" %% "shapeless" % "2.3.1"
  )

  val kafka = Seq(
    "org.apache.kafka" %% "kafka"                % "0.9.0.1",
    "io.confluent"     % "kafka-avro-serializer" % "1.0",
    "org.apache.avro"  % "avro"                  % "1.8.1"
  )
}
