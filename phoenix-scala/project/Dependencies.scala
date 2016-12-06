import sbt._

object Versions {
  val scala = "2.11.8"
  // This is a patched version found here:
  // https://github.com/kwark/slick/blob/3.1-deadlock/README.md
  // Fixes a critical deadlock in slick.
  // Change once lands in mainline.
  val slick     = "3.1.1.2"
  val json4s    = "3.4.0"
  val akka      = "2.4.7"
  val slickPg   = "0.14.2"
  val gatling   = "2.2.1"
  val scalatest = "2.2.6"
  val dispatch  = "0.11.3"
  val fasterxml = "2.8.2"

}

object Dependencies {

  val akka = Seq(
    "com.typesafe.akka" %% "akka-slf4j"     % Versions.akka,
    "com.typesafe.akka" %% "akka-actor"     % Versions.akka,
    "com.typesafe.akka" %% "akka-agent"     % Versions.akka,
    "com.typesafe.akka" %% "akka-stream"    % Versions.akka,
    "com.typesafe.akka" %% "akka-http-core" % Versions.akka,
    "de.heikoseeberger" %% "akka-sse"       % "1.8.1"
  )

  val slick = Seq(
    "com.typesafe.slick" %% "slick"          % Versions.slick,
    "com.typesafe.slick" %% "slick-hikaricp" % Versions.slick
  )

  val fasterxml = Seq(
    "com.fasterxml.jackson.dataformat" % "jackson-dataformat-smile" % Versions.fasterxml
  )

  val json4s = Seq(
    "org.json4s"        %% "json4s-core"      % Versions.json4s,
    "org.json4s"        %% "json4s-jackson"   % Versions.json4s,
    "org.json4s"        %% "json4s-ext"       % Versions.json4s,
    "de.heikoseeberger" %% "akka-http-json4s" % "1.7.0"
  )

  val gatling = Seq(
    "io.gatling"            % "gatling-app"               % Versions.gatling,
    "io.gatling.highcharts" % "gatling-charts-highcharts" % Versions.gatling
  )

  val db = Seq(
    "com.github.tminglei" %% "slick-pg"         % Versions.slickPg,
    "com.github.tminglei" %% "slick-pg_json4s"  % Versions.slickPg,
    "com.zaxxer"          % "HikariCP"          % "2.4.7" % "provided",
    "org.postgresql"      % "postgresql"        % "9.4.1208",
    "org.flywaydb"        % "flyway-core"       % "4.0.3",
    "com.github.mauricio" %% "postgresql-async" % "0.2.20"
  )

  val apis = Seq(
    "com.sksamuel.elastic4s" %% "elastic4s-core" % "2.3.0",
    "com.amazonaws"          % "aws-java-sdk"    % "1.11.15",
    "com.stripe"             % "stripe-java"     % "2.7.0"
  )

  val logging = Seq(
    "ch.qos.logback"             % "logback-classic" % "1.1.7",
    "com.typesafe.scala-logging" %% "scala-logging"  % "3.4.0",
    "com.lihaoyi"                %% "sourcecode"     % "0.1.1",
    "org.slf4j"                  % "slf4j-api"       % "1.7.21"
  )

  val test = Seq(
    "org.scalatest"     %% "scalatest"           % Versions.scalatest,
    "org.scalacheck"    %% "scalacheck"          % "1.13.1",
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
    "com.wix"               %% "accord-core"            % "0.5", // Validation
    "com.networknt"         % "json-schema-validator"   % "0.1.1",
    "com.github.scopt"      %% "scopt"                  % "3.5.0", // CLI args
    ("org.joda"             % "joda-money"              % "0.11").exclude("org.joda", "joda-time"),
    "com.chuusai"           %% "shapeless"              % "2.3.1",
    "com.pellucid"          %% "sealerate"              % "0.0.3",
    "it.justwrote"          %% "scala-faker"            % "0.3",
    "org.conbere"           % "markov_2.10"             % "0.2.0",
    "io.backchat.inflector" %% "scala-inflector"        % "1.3.5",
    "com.github.tototoshi"  %% "scala-csv"              % "1.3.3",
    "org.typelevel"         %% "cats"                   % "0.7.2"
  )

  private lazy val noScalaCheck = ExclusionRule(organization = "org.scalacheck")

}
