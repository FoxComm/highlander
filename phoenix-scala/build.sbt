import scala.io.Source.fromFile

import Configurations._
import Dependencies.baseDependencies
import Settings._
import Tasks._

scalaVersion in ThisBuild := Versions.scala

scalaOrganization in ThisBuild := "org.typelevel"

lazy val phoenix = (project in file("phoenix"))
  .dependsOn(core, objectframework)
  .configs(IT, ET)
  .settings(itSettings, etSettings)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= {
      import Dependencies._
      baseDependencies ++ akka ++ http ++ auth ++ fasterxml ++ apis ++ test ++ misc ++ kafka
    },
    (mainClass in Compile) := Some("phoenix.server.Main"),
    // TODO @anna move the rest of location settings to common when tests are moved into subprojects
    scalaSource in Test    := baseDirectory.value / "test" / "unit",
    scalaSource in IT      := baseDirectory.value / "test" / "integration",
    scalaSource in ET      := baseDirectory.value / "test" / "integration",
    resourceDirectory in Test    := baseDirectory.value / "test" / "resources",
    resourceDirectory in IT      := (resourceDirectory in Test).value,
    resourceDirectory in ET      := (resourceDirectory in Test).value,
    initialCommands in console := fromFile("project/console_init").getLines.mkString("\n"),
    initialCommands in (Compile, consoleQuick) := "",
    test := Def.sequential(compile in Test, compile in IT, compile in ET,
                           test    in Test, test    in IT, test    in ET).value,
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
  .aggregate(phoenix, seeder)
  .settings(commonSettings)
  .settings(
    name := "phoenix-scala",
    writeVersion := sh.toTask(fromFile("project/write_version").getLines.mkString).value,
    unmanagedResources in Compile += file("version"),
    addCommandAlias("all", "; clean; phoenix/it:compile; test; assembly"),
    addCommandAlias("console", "phoenix/console")
  )

lazy val seeder = (project in file("seeder"))
  .dependsOn(phoenix)
  .settings(
    commonSettings,
    libraryDependencies ++= Dependencies.gatling,
    cleanFiles += baseDirectory.value / "results",
    // we cannot fork and set javaOptions simply, as it causes some weird issue with db schema creation
    initialize ~= (_ => System.setProperty("phoenix.env", "test" )),
    fullClasspath in assembly := { // thanks sbt for that hacky way of excluding inter-project dependencies
      val phoenixClasses = (crossTarget in compile in phoenix).value.getAbsolutePath
      (fullClasspath in assembly).value.filterNot(_.data.getAbsolutePath.startsWith(phoenixClasses))
    },
    assemblyExcludedJars in assembly := (fullClasspath in assembly in phoenix).value
  )

lazy val objectframework = (project in file("objectframework"))
  .dependsOn(core)
  .settings(
    commonSettings,
    libraryDependencies ++= baseDependencies,
    libraryDependencies += "com.networknt" % "json-schema-validator" % "0.1.1"
  )

lazy val core = (project in file("core"))
  .settings(
    commonSettings,
    libraryDependencies ++= baseDependencies
  )

fullAssembly := Def.task().dependsOn(writeVersion in root, assembly in phoenix, assembly in seeder).value

// Injected seeds
val seedCommand = " seeds.Seeds seed --seedAdmins"
seed     := (runMain in Compile in seeder).partialInput(seedCommand).evaluated
seedDemo := (runMain in Compile in seeder).partialInput(s"$seedCommand --seedDemo 1").evaluated

// Gatling seeds
seedOneshot    := (runMain in Compile in seeder).partialInput(" gatling.seeds.OneshotSeeds").evaluated
seedContinuous := (runMain in Compile in seeder).partialInput(" gatling.seeds.ContinuousSeeds").evaluated
