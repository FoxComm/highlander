import sbtassembly.AssemblyKeys

lazy val commonSettings = Seq(
  version       := "1.0",
  scalaVersion  := "2.11.8",
  updateOptions := updateOptions.value.withCachedResolution(true),
  scalacOptions ++= List(
    "-encoding", "UTF-8",
    "-target:jvm-1.8",
    "-feature",
    "-unchecked",
    "-deprecation",
    "-Xfatal-warnings",
    "-language:higherKinds",
    "-language:existentials",
    "-Ywarn-numeric-widen",
    "-Ywarn-nullary-override",
    "-Ywarn-nullary-unit",
    "-Ywarn-infer-any"
  )
)

scapegoatVersion := "1.2.1"

lazy val slickV = "3.1.1"

lazy val phoenixScala = (project in file(".")).
  settings(commonSettings).
  configs(IT).
  settings(inConfig(IT)(Defaults.testSettings)).
  settings(
    name      := "phoenix-scala",
    version   := "1.0",
    /** Work around SBT warning for multiple dependencies */
    dependencyOverrides ++= Set(
      "org.scala-lang"         % "scala-library"              % scalaVersion.value,
      "com.typesafe.slick"     %% "slick"                     % slickV,
      "com.typesafe.slick"     %% "slick-hikaricp"            % slickV
    ),
    ivyScala            := ivyScala.value.map(_.copy(overrideScalaVersion = true)),
    resolvers ++= Seq(
      "hseeberger bintray" at "http://dl.bintray.com/hseeberger/maven",
      "pellucid bintray"   at "http://dl.bintray.com/pellucid/maven",
      "justwrote"          at "http://repo.justwrote.it/releases/"
    ),
    libraryDependencies ++= {
      val akkaV      = "2.4.4"
      val scalaTestV = "2.2.6"
      val json4sV    = "3.3.0"
      val slickPgV   = "0.13.0"

      Seq(
        // Akka
        "com.typesafe.akka"          %% "akka-slf4j"             % akkaV,
        "com.typesafe.akka"          %% "akka-actor"             % akkaV,
        "com.typesafe.akka"          %% "akka-agent"             % akkaV,
        "com.typesafe.akka"          %% "akka-stream"            % akkaV,
        "com.typesafe.akka"          %% "akka-http-core"         % akkaV,
        "de.heikoseeberger"          %% "akka-sse"               % "1.7.3",
        // http
        "net.databinder.dispatch"    %% "dispatch-core"          % "0.11.3",
        "net.databinder.dispatch"    %% "dispatch-json4s-native" % "0.11.3",
        // JSON
        "org.json4s"                 %% "json4s-core"            % json4sV,
        "org.json4s"                 %% "json4s-jackson"         % json4sV,
        "org.json4s"                 %% "json4s-ext"             % json4sV,
        "de.heikoseeberger"          %% "akka-http-json4s"       % "1.6.0",
        // Database
        "com.typesafe.slick"         %% "slick"                  % slickV,
        "com.typesafe.slick"         %% "slick-hikaricp"         % slickV,
        "com.github.tminglei"        %% "slick-pg"               % slickPgV,
        "com.github.tminglei"        %% "slick-pg_json4s"        % slickPgV,
        "com.zaxxer"                 %  "HikariCP"               % "2.4.5"    % "provided",
        "org.postgresql"             %  "postgresql"             % "9.4.1208",
        "org.flywaydb"               %  "flyway-core"            % "4.0",
        "com.github.mauricio"        %% "postgresql-async"       % "0.2.19",
        // Elasticsearch
        "com.sksamuel.elastic4s"     %% "elastic4s-core"         % "2.3.0"    % "provided",
        // Validations
        "com.wix"                    %% "accord-core"            % "0.5",
        // Auth
        "org.bitbucket.b_c"          %  "jose4j"                 % "0.5.0",
        "com.lambdaworks"            %  "scrypt"                 % "1.4.0",
        // Logging
        "ch.qos.logback"             %  "logback-classic"        % "1.1.7",
        "com.typesafe.scala-logging" %% "scala-logging"          % "3.4.0",
        "com.lihaoyi"                %% "sourcecode"             % "0.1.1",
        // Other
       ("org.spire-math"             %% "cats"                   % "0.3.0").excludeAll(noScalaCheckPlease),
        "com.stripe"                 %  "stripe-java"            % "2.4.0",
        "org.slf4j"                  %  "slf4j-api"              % "1.7.21",
        "org.joda"                   %  "joda-money"             % "0.11",
        "com.pellucid"               %% "sealerate"              % "0.0.3",
        "com.chuusai"                %% "shapeless"              % "2.3.0",
        "it.justwrote"               %% "scala-faker"            % "0.3",
        "io.backchat.inflector"      %% "scala-inflector"        % "1.3.5",
        "com.github.tototoshi"       %% "scala-csv"              % "1.3.1",
        // Testing
        "org.conbere"                %  "markov_2.10"            % "0.2.0",
        "com.typesafe.akka"          %% "akka-testkit"           % akkaV      % "test",
        "com.typesafe.akka"          %% "akka-stream-testkit"    % akkaV      % "test",
        "org.scalatest"              %% "scalatest"              % scalaTestV % "test",
        "org.scalacheck"             %% "scalacheck"             % "1.13.1"   % "test",
        "org.mockito"                %  "mockito-core"           % "1.10.19"  % "test")
    },
    scalaSource in Compile <<= (baseDirectory in Compile)(_ / "app"),
    scalaSource in Test <<= (baseDirectory in Test)(_ / "test" / "unit"),
    scalaSource in IT   <<= (baseDirectory in Test)(_ / "test" / "integration"),
    resourceDirectory in Compile := baseDirectory.value / "resources",
    resourceDirectory in Test := baseDirectory.value / "test" / "resources",
    resourceDirectory in IT   := baseDirectory.value / "test" / "resources",
    Revolver.settings,
    (mainClass in Compile) := Some("server.Main"),
    initialCommands in console :=
      """
        |import scala.concurrent.ExecutionContext.Implicits.global
        |import slick.driver.PostgresDriver.api._
        |import models._
        |val config: com.typesafe.config.Config = utils.FoxConfig.loadWithEnv()
        |implicit val db = Database.forConfig("db", config)
        |import utils.db._
        """.stripMargin,
    initialCommands in (Compile, consoleQuick) := "",
    // add ms report for every test
    testOptions in Test ++= Seq(Tests.Argument(TestFrameworks.ScalaTest, "-oD"), Tests.Argument("-l", "tags.External")),
    javaOptions in Test ++= Seq("-Xmx2G", "-XX:+UseConcMarkSweepGC", "-Dphoenix.env=test"),
    parallelExecution in Test := true,
    parallelExecution in IT   := false,
    fork in Test := false,
    fork in IT   := true, /** FIXME: We couldn’t run ITs in parallel if we fork */
    test in assembly := {},
    addCommandAlias("assembly", "gatling/assembly"),
    addCommandAlias("all", "; clean; gatling/clean; it:compile; gatling/compile; test; gatling/assembly"),
    test <<= Def.task {
      /** We need to do nothing here. Unit and ITs will run in parallel
        * and this task will fail if any of those fail. */
      ()
    }.dependsOn(test in Test, test in IT)
)

lazy val IT = config("it") extend Test

lazy val seed = inputKey[Unit]("Resets and seeds the database")
seed := { (runMain in Compile).partialInput(" utils.seeds.Seeds").evaluated }

/** Cats pulls in disciple which pulls in scalacheck, and SBT will notice and set up a test for ScalaCheck */
lazy val noScalaCheckPlease: ExclusionRule = ExclusionRule(organization = "org.scalacheck")

lazy val gatling = (project in file("gatling")).
  dependsOn(phoenixScala).
  settings(
    commonSettings,
    libraryDependencies ++= {
      val gatlingV = "2.1.7"
      Seq(
        "io.gatling"            % "gatling-app"               % gatlingV,
        "io.gatling.highcharts" % "gatling-charts-highcharts" % gatlingV
      )
    },
    classDirectory in Compile := baseDirectory.value / "../gatling-classes",
    mainClass in Compile := Some("seeds.GatlingApp"),
    cleanFiles <+= baseDirectory(_ / "../gatling-classes"),
    cleanFiles <+= baseDirectory(_ / "../gatling-results"),
    assemblyJarName := (AssemblyKeys.assemblyJarName in assembly in phoenixScala).value
  )

lazy val seedGatling = inputKey[Unit]("Seed DB with Gatling")
seedGatling := { (runMain in Compile in gatling).partialInput(" seeds.GatlingApp").evaluated }
