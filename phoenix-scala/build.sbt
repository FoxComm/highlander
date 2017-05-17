import scala.io.Source.fromFile

import Configurations._
import Settings._
import Tasks._

scalaVersion in ThisBuild := Versions.scala

scalaOrganization in ThisBuild := "org.typelevel"

lazy val phoenix = (project in file("phoenix"))
  .dependsOn(starfish, objectframework)
  .configs(IT, ET)
  .settings(itSettings, etSettings)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= {
      import Dependencies._
      akka ++ http ++ auth ++ json4s ++ fasterxml ++ apis ++ logging ++ test ++ misc ++ kafka
    },
    (mainClass in Compile) := Some("server.Main"),
    // TODO @anna move the rest of location settings to common when tests are moved into subprojects
    scalaSource in Test    := baseDirectory.value / "test" / "unit",
    scalaSource in IT      := baseDirectory.value / "test" / "integration",
    scalaSource in ET      := baseDirectory.value / "test" / "integration",
    resourceDirectory in Test    := baseDirectory.value / "test" / "resources",
    resourceDirectory in IT      := (resourceDirectory in Test).value,
    resourceDirectory in ET      := (resourceDirectory in Test).value,
    testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oD"),
    javaOptions in Test ++= Seq("-Xmx2G", "-XX:+UseConcMarkSweepGC", "-Dphoenix.env=test"),
    parallelExecution in Compile := true,
    parallelExecution in Test := true,
    parallelExecution in IT   := false,
    parallelExecution in ET   := false,
    fork in Test := false,
    fork in IT   := true, /** FIXME: We couldn’t run ITs in parallel if we fork */
    fork in ET   := true,
    logBuffered in Test := false,
    logBuffered in IT   := false,
    logBuffered in ET   := false
  )

lazy val root = (project in file("."))
  .aggregate(phoenix, objectframework, starfish)
  .settings(commonSettings)
  .settings(
    name := "phoenix-scala",
    initialCommands in console := fromFile("project/console_init").getLines.mkString("\n"),
    initialCommands in (Compile, consoleQuick) := "",
    writeVersion := sh.toTask(fromFile("project/write_version").getLines.mkString).value,
    unmanagedResources in Compile += file("version"),
    test in assembly := {},
    addCommandAlias("all", "; clean; seeder/clean; it:compile; seeder/compile; test; seeder/assembly"),
    assemblyMergeStrategy in assembly := {
      case PathList("org", "joda", "time", xs @ _ *) ⇒
        MergeStrategy.first
      case PathList("org", "slf4j", xs @ _ *) ⇒
        MergeStrategy.first
      case PathList("ch", "qos", "logback", xs @ _ *) ⇒
        MergeStrategy.first
      case PathList("scala", xs @ _ *) ⇒ // FIXME: investigate what’s still pulling in Lightbend Scala?
        MergeStrategy.first
      case PathList("library.properties", xs @ _ *) ⇒ // FIXME: investigate what’s still pulling in Lightbend Scala?
        MergeStrategy.first
      case x ⇒
        (assemblyMergeStrategy in assembly).value.apply(x)
    }
  )

lazy val seeder = (project in file("seeder"))
  .dependsOn(phoenix)
  .settings(
    commonSettings,
    libraryDependencies ++= Dependencies.gatling,
    cleanFiles += baseDirectory.value / "results",
    // we cannot fork and set javaOptions simply, as it causes some weird issue with db schema creation
    initialize ~= (_ => System.setProperty("phoenix.env", "test" )),
    assemblyMergeStrategy in assembly := {
      case PathList("org", "joda", "time", xs @ _ *) ⇒
        MergeStrategy.first
      case PathList("org", "slf4j", xs @ _ *) ⇒
        MergeStrategy.first
      case PathList("ch", "qos", "logback", xs @ _ *) ⇒
        MergeStrategy.first
      case PathList("io", "netty", xs @ _ *) ⇒
        MergeStrategy.first
      case PathList("META-INF", "io.netty.versions.properties") ⇒
        MergeStrategy.first
      case x ⇒
        (assemblyMergeStrategy in assembly).value.apply(x)
    },
    fullClasspath in assembly := { // thanks sbt for that hacky way of excluding inter-project dependencies
      val phoenixClasses = (crossTarget in compile in phoenix).value.getAbsolutePath
      (fullClasspath in assembly).value.filterNot(_.data.getAbsolutePath.startsWith(phoenixClasses))
    },
    assemblyExcludedJars in assembly := (fullClasspath in assembly in phoenix).value
  )

lazy val objectframework = (project in file("objectframework"))
  .dependsOn(starfish)
  .settings(
    commonSettings,
    libraryDependencies ++= {
      import Dependencies._
      cats ++ shapeless ++ db ++ slick ++ json4s ++ logging :+
      "com.networknt"         % "json-schema-validator"   % "0.1.1"
    }
  )

lazy val starfish = (project in file("starfish"))
  .settings(
    commonSettings,
    libraryDependencies ++= {
      import Dependencies._
      cats ++ shapeless ++ db ++ slick ++ json4s
    }
  )

fullAssembly := Def.task().dependsOn(writeVersion in root, assembly in phoenix, assembly in seeder).value

// Injected seeds
val seedCommand = " utils.seeds.Seeds seed --seedAdmins"
seed     := (runMain in Compile in seeder).partialInput(seedCommand).evaluated
seedDemo := (runMain in Compile in seeder).partialInput(s"$seedCommand --seedDemo 1").evaluated

// Gatling seeds
seedOneshot    := (runMain in Compile in seeder).partialInput(" gatling.seeds.OneshotSeeds").evaluated
seedContinuous := (runMain in Compile in seeder).partialInput(" gatling.seeds.ContinuousSeeds").evaluated

// Scalafmt
scalafmtAll := Def.task().dependsOn(scalafmt in Compile in phoenix,
                                    scalafmt in Test    in phoenix,
                                    scalafmt in IT      in phoenix,
                                    scalafmt in ET      in phoenix,
                                    scalafmt in Compile in objectframework,
                                    scalafmt in Compile in starfish,
                                    scalafmt in Compile in seeder).value

scalafmtTestAll := Def.task().dependsOn(scalafmtTest in Compile in phoenix,
                                        scalafmtTest in Test    in phoenix,
                                        scalafmtTest in IT      in phoenix,
                                        scalafmtTest in ET      in phoenix,
                                        scalafmtTest in Compile in objectframework,
                                        scalafmtTest in Compile in starfish,
                                        scalafmtTest in Compile in seeder).value

// Test
test := Def.sequential(compile in Test, compile in IT, compile in ET,
                        test   in Test, test    in IT, test    in ET).value
