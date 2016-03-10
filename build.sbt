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
    "-Xlint",
    "-Xfatal-warnings",
    "-language:higherKinds",
    "-language:existentials",
    "-Ywarn-numeric-widen",
    "-Ywarn-nullary-override",
    "-Ywarn-nullary-unit",
    "-Ywarn-infer-any"
  )
)


lazy val testWartWarnings = Seq(Wart.OptionPartial)

lazy val slickV = "3.1.1"

lazy val phoenixScala = (project in file(".")).
  settings(commonSettings).
  configs(IT).
  settings(inConfig(IT)(Defaults.testSettings)).
  settings(inConfig(Test)(wartremoverWarnings ++= testWartWarnings)).
  settings(inConfig(IT)(wartremoverWarnings ++= testWartWarnings)).
  settings(
    wartremoverWarnings in (Compile, compile) ++=
      Warts.allBut(
        /** Covered by the compiler */
        Wart.Any,

        /** In the absence of type annotations, Good(v: A) is inferred as Or[A, Nothing] */
        Wart.Nothing,

        /** Many library methods can throw, for example Future.map, but not much
          * 3-rd party code uses @throws */
        Wart.Throw,

        /** Good is a case class and therefore has Product and Serializable */
        Wart.Product, Wart.Serializable,

        /**
         * Can’t figure out how to resolve this issue.
         *
         * {{{
         *   app/models/Address.scala:48: Statements must return Unit
         *   [error] object Addresses extends TableQueryWithId[Address, Addresses](
         *                                    ^
         * }}}
         */
        Wart.NonUnitStatements,

        /** This goes overboard. Wart remover’s justification is that those are hard to be used as functions. */
        Wart.DefaultArguments,

        /** [[scala.collection.JavaConverters]] does not have methods for handling Maps */
        Wart.JavaConversions,

        /** While Applicatives might be simpler, there is no for comprehension sugar for them. **/
        Wart.NoNeedForMonad,

        /** New warts from version 0.14 **/
        Wart.ToString,
        Wart.ExplicitImplicitTypes
      )
  ).
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
      val akkaV      = "2.4.2"
      val scalaTestV = "2.2.6"
      val monocleV   = "1.2.0"
      val json4sV    = "3.3.0"
      val logbackV   = "1.1.5"
      val skickPgV    = "0.12.0"

      Seq(
        // Akka
        "com.typesafe.akka"          %% "akka-slf4j"             % akkaV,
        "com.typesafe.akka"          %% "akka-actor"             % akkaV,
        "com.typesafe.akka"          %% "akka-agent"             % akkaV,
        "com.typesafe.akka"          %% "akka-stream"            % akkaV,
        "com.typesafe.akka"          %% "akka-http-core"         % akkaV,
        "de.heikoseeberger"          %% "akka-sse"               % "1.6.3",
        // JSON
        "org.json4s"                 %% "json4s-core"            % json4sV,
        "org.json4s"                 %% "json4s-jackson"         % json4sV,
        "org.json4s"                 %% "json4s-ext"             % json4sV,
        "de.heikoseeberger"          %% "akka-http-json4s"       % "1.5.2",
        // Database
        "com.typesafe.slick"         %% "slick"                  % slickV,
        "com.typesafe.slick"         %% "slick-hikaricp"         % slickV,
        "com.github.tminglei"        %% "slick-pg"               % skickPgV,
        "com.github.tminglei"        %% "slick-pg_json4s"        % skickPgV,
        "com.zaxxer"                 %  "HikariCP"               % "2.4.3"    % "provided",
        "org.postgresql"             %  "postgresql"             % "9.4.1208",
        "org.flywaydb"               %  "flyway-core"            % "3.2.1",
        "com.github.mauricio"        %% "postgresql-async"       % "0.2.18",
        // Validations
        "com.wix"                    %% "accord-core"            % "0.5",
        "org.scalactic"              %% "scalactic"              % "2.2.6",
        // Auth
        "org.bitbucket.b_c"          %  "jose4j"                  % "0.4.4",
        "com.lambdaworks"            %  "scrypt"                 % "1.4.0",
        // Logging
        "ch.qos.logback"             %  "logback-core"           % logbackV,
        "ch.qos.logback"             %  "logback-classic"        % logbackV,
        // Other
       ("org.spire-math"             %% "cats"                   % "0.3.0").excludeAll(noScalaCheckPlease),
        "com.stripe"                 %  "stripe-java"            % "1.45.0",
        "org.slf4j"                  %  "slf4j-api"              % "1.7.16",
        "org.joda"                   %  "joda-money"             % "0.11",
        "com.pellucid"               %% "sealerate"              % "0.0.3",
        "com.github.julien-truffaut" %% "monocle-core"           % monocleV,
        "com.github.julien-truffaut" %% "monocle-generic"        % monocleV,
        "com.github.julien-truffaut" %% "monocle-macro"          % monocleV,
        "it.justwrote"               %% "scala-faker"            % "0.3",
        "io.backchat.inflector"      %% "scala-inflector"        % "1.3.5",
        // Testing
        "org.conbere"                %  "markov_2.10"            % "0.2.0",
        "com.typesafe.akka"          %% "akka-testkit"           % akkaV      % "test",
        "com.typesafe.akka"          %% "akka-stream-testkit"    % akkaV      % "test",
        "org.scalatest"              %% "scalatest"              % scalaTestV % "test",
        "org.scalacheck"             %% "scalacheck"             % "1.13.0"   % "test",
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
        |val config: com.typesafe.config.Config = utils.Config.loadWithEnv()
        |implicit val db = Database.forConfig("db", config)
        |import utils.Slick.implicits._
        """.stripMargin,
    initialCommands in (Compile, consoleQuick) := "",
    // add ms report for every test
    testOptions in Test ++= Seq(Tests.Argument(TestFrameworks.ScalaTest, "-oD"), Tests.Argument("-l", "tags.External")),
    javaOptions in Test ++= Seq("-Xmx2G", "-XX:+UseConcMarkSweepGC"),
    parallelExecution in Test := true,
    parallelExecution in IT   := false,
    fork in Test := false,
    fork in IT   := true, /** FIXME: We couldn’t run ITs in parallel if we fork */
    test in assembly := {},
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
