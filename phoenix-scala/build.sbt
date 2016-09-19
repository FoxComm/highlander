import sbtassembly.AssemblyKeys

lazy val commonSettings = Seq(
  scalaVersion     := "2.11.8",
  updateOptions    := updateOptions.value.withCachedResolution(true),
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

lazy val scalafmtAll     = taskKey[Unit]("scalafmt all the things")
lazy val scalafmtTestAll = taskKey[Unit]("scalafmtTest all the things")

val sharedItSettings = Defaults.testTasks ++ Defaults.itSettings ++ ScalaFmtPlugin.configScalafmtSettings

lazy val itSettings = inConfig(IT)(sharedItSettings) ++ Seq(
  testOptions in IT := (testOptions in Test).value :+ Tests.Argument("-l", "tags.External")
)

lazy val etSettings = inConfig(ET)(sharedItSettings) ++ Seq(
  testOptions in ET := (testOptions in Test).value :+ Tests.Argument("-n", "tags.External")
)

lazy val writeVersion = taskKey[Seq[String]]("Write project version data to version file")

lazy val phoenixScala = (project in file(".")).
  settings(commonSettings).
  configs(IT, ET).
  settings(itSettings, etSettings).
  settings(
    name      := "phoenix-scala",
    version   := "1.0",
    /** Work around SBT warning for multiple dependencies */
    dependencyOverrides += "org.scala-lang" % "scala-library" % scalaVersion.value,
    dependencyOverrides ++= Dependencies.slick.toSet,
    dependencyOverrides ++= Dependencies.json4s.toSet,
    ivyScala            := ivyScala.value.map(_.copy(overrideScalaVersion = true)),
    resolvers ++= Seq(
      "hseeberger bintray" at "http://dl.bintray.com/hseeberger/maven",
      "pellucid bintray"   at "http://dl.bintray.com/pellucid/maven",
      "justwrote"          at "http://repo.justwrote.it/releases/",
      Resolver.bintrayRepo("kwark", "maven") //This is a fix for deadlock in slick.
                                             // to official slick repo.
    ),
    libraryDependencies ++= Dependencies.akka,
    libraryDependencies ++= Dependencies.slick,
    libraryDependencies ++= Dependencies.json4s,
    libraryDependencies ++= {
      val test = "test,it,et"

      val scalaTestV = "2.2.6"
      val slickPgV   = "0.14.2"

      Seq(
        // http
        "net.databinder.dispatch"    %% "dispatch-core"          % "0.11.3",
        "net.databinder.dispatch"    %% "dispatch-json4s-native" % "0.11.3",
        // Database
        "com.github.tminglei"        %% "slick-pg"               % slickPgV,
        "com.github.tminglei"        %% "slick-pg_json4s"        % slickPgV,
        "com.zaxxer"                 %  "HikariCP"               % "2.4.7"  % "provided",
        "org.postgresql"             %  "postgresql"             % "9.4.1208",
        "org.flywaydb"               %  "flyway-core"            % "4.0.3",
        "com.github.mauricio"        %% "postgresql-async"       % "0.2.20",
        // Elasticsearch
        "com.sksamuel.elastic4s"     %% "elastic4s-core"         % "2.3.0",
        // Validations
        "com.wix"                    %% "accord-core"            % "0.5",
        // Auth
        "org.bitbucket.b_c"          %  "jose4j"                 % "0.5.1",
        "com.lambdaworks"            %  "scrypt"                 % "1.4.0",
        // Logging
        "ch.qos.logback"             %  "logback-classic"        % "1.1.7",
        "com.typesafe.scala-logging" %% "scala-logging"          % "3.4.0",
        "com.lihaoyi"                %% "sourcecode"             % "0.1.1",
        // CLI parsing
        "com.github.scopt"           %% "scopt"                  % "3.5.0",
        // Other
       ("org.spire-math"             %% "cats"                   % "0.3.0").excludeAll(noScalaCheckPlease),
        "com.stripe"                 %  "stripe-java"            % "2.7.0",
        "org.slf4j"                  %  "slf4j-api"              % "1.7.21",
        "org.joda"                   %  "joda-money"             % "0.11",
        "com.pellucid"               %% "sealerate"              % "0.0.3",
        "com.chuusai"                %% "shapeless"              % "2.3.1",
        "it.justwrote"               %% "scala-faker"            % "0.3",
        "io.backchat.inflector"      %% "scala-inflector"        % "1.3.5",
        "com.github.tototoshi"       %% "scala-csv"              % "1.3.3",
        "com.amazonaws"              %  "aws-java-sdk"           % "1.11.15",
        // Testing
        "org.conbere"                %  "markov_2.10"            % "0.2.0",
        "org.scalatest"              %% "scalatest"              % scalaTestV       % test,
        "org.scalacheck"             %% "scalacheck"             % "1.13.1"         % test,
        "org.mockito"                %  "mockito-core"           % "2.1.0-beta.125" % test)
    },
    scalaSource in Compile <<= baseDirectory(_ / "app"),
    scalaSource in Test    <<= baseDirectory(_ / "test" / "unit"),
    scalaSource in IT      <<= baseDirectory(_ / "test" / "integration"),
    scalaSource in ET      <<= baseDirectory(_ / "test" / "integration"),
    resourceDirectory in Compile <<= baseDirectory(_ / "resources"),
    resourceDirectory in Test    <<= baseDirectory(_ / "test" / "resources"),
    resourceDirectory in IT      <<= resourceDirectory in Test,
    resourceDirectory in ET      <<= resourceDirectory in Test,
    Revolver.settings,
    (mainClass in Compile) := Some("server.Main"),
    initialCommands in console :=
      """
        |import scala.concurrent.ExecutionContext.Implicits.global
        |import slick.driver.PostgresDriver.api._
        |import models._
        |import scala.concurrent.{Await, Future}
        |import scala.concurrent.duration._
        |import models.activity.ActivityContext
        |implicit val ac = ActivityContext(userId = 1, userType = "admin", transactionId = utils.generateUuid)
        |final implicit class ConsoleEnrichedFuture[A](val future: Future[A]) extends AnyVal {
        |  def get(): A = Await.result(future, 1.minute)
        |}
        |val config: com.typesafe.config.Config = utils.FoxConfig.loadWithEnv()
        |implicit val db = Database.forConfig("db", config)
        |import utils.db._
        """.stripMargin,
    initialCommands in (Compile, consoleQuick) := "",
    writeVersion <<= sh.toTask(
      """
        |echo -n "Build date: "      >  version &&
        |date                        >> version &&
        |echo                        >> version &&
        |echo Git HEAD commit:       >> version &&
        |git show HEAD --pretty=full >> version
      """.stripMargin).triggeredBy(compile in Compile),
    testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oD"),
    javaOptions in Test ++= Seq("-Xmx2G", "-XX:+UseConcMarkSweepGC", "-Dphoenix.env=test"),
    parallelExecution in Test := true,
    parallelExecution in IT   := false,
    parallelExecution in ET   := false,
    fork in Test := false,
    fork in IT   := true, /** FIXME: We couldn’t run ITs in parallel if we fork */
    fork in ET   := true,
    logBuffered in Test := false,
    logBuffered in IT   := false,
    logBuffered in ET   := false,
    test in assembly := {},
    addCommandAlias("assembly", "gatling/assembly"),
    addCommandAlias("all", "; clean; gatling/clean; it:compile; gatling/compile; test; gatling/assembly"),
    scalafmtConfig := Some(file(".scalafmt")),
    reformatOnCompileWithItSettings, // scalafmt
    scalafmtAll     <<= Def.task().dependsOn(scalafmt in Compile,
                                             scalafmt in Test,
                                             scalafmt in IT,
                                             scalafmt in ET),
    scalafmtTestAll <<= Def.task().dependsOn(scalafmtTest in Compile,
                                             scalafmtTest in Test,
                                             scalafmtTest in IT,
                                             scalafmtTest in ET),
    test <<= Def.sequential(compile in Test, compile in IT, compile in ET,
                            test    in Test, test    in IT, test    in ET)
  )

lazy val IT = config("it") extend Test

// ET == external tests
lazy val ET = config("et") extend IT

lazy val seed = inputKey[Unit]("Resets and seeds the database")
seed := { (runMain in Compile).partialInput(" utils.seeds.Seeds seed --seedAdmins --seedDemo 1").evaluated }

/** Cats pulls in disciple which pulls in scalacheck, and SBT will notice and set up a test for ScalaCheck */
lazy val noScalaCheckPlease: ExclusionRule = ExclusionRule(organization = "org.scalacheck")

lazy val gatling = (project in file("gatling")).
  dependsOn(phoenixScala).
  settings(
    commonSettings,
    libraryDependencies ++= {
      val gatlingV = "2.2.1"
      Seq(
        "io.gatling"            % "gatling-app"               % gatlingV,
        "io.gatling.highcharts" % "gatling-charts-highcharts" % gatlingV
      )
    },
    classDirectory in Compile := baseDirectory.value / "../gatling-classes",
    cleanFiles <+= baseDirectory(_ / "../gatling-classes"),
    cleanFiles <+= baseDirectory(_ / "../gatling-results"),
    assemblyJarName := (AssemblyKeys.assemblyJarName in assembly in phoenixScala).value,
    scalafmtConfig := Some(file(".scalafmt")),
    reformatOnCompileSettings, // scalafmt
    assemblyMergeStrategy in assembly := {
      case PathList("org", "joda", "time", xs@_*) ⇒
        MergeStrategy.first
      case PathList("io", "netty", xs@_*) ⇒
        MergeStrategy.first
      case PathList("META-INF", "io.netty.versions.properties") ⇒
        MergeStrategy.first
      case PathList("META-INF", "native", "libnetty-transport-native-epoll.so") ⇒
        MergeStrategy.first
      case x ⇒
        (assemblyMergeStrategy in assembly).value.apply(x)
    }
  )

lazy val seedOneshot = inputKey[Unit]("Run oneshot gatling seeds")
seedOneshot := { (runMain in Compile in gatling).partialInput(" seeds.OneshotSeeds").evaluated }

lazy val seedContinuous = inputKey[Unit]("Run continuous gatling seeds")
seedContinuous := { (runMain in Compile in gatling).partialInput(" seeds.ContinuousSeeds").evaluated }
