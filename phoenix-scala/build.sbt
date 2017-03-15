import scala.io.Source.fromFile

import Configurations._
import Settings._
import Tasks._

scalaVersion in ThisBuild := Versions.scala

scalaOrganization in ThisBuild := "org.typelevel"

lazy val phoenixScala = (project in file("."))
  .settings(commonSettings)
  .configs(IT, ET)
  .settings(itSettings, etSettings)
  .settings(
    name := "phoenix-scala",
    /** Work around SBT warning for multiple dependencies */
    dependencyOverrides += "org.scala-lang" % "scala-library" % scalaVersion.value,
    dependencyOverrides ++= Dependencies.slick.toSet,
    dependencyOverrides ++= Dependencies.json4s.toSet,
    ivyScala := ivyScala.value.map(_.copy(overrideScalaVersion = true)),
    resolvers ++= Seq(
      "hseeberger bintray" at "http://dl.bintray.com/hseeberger/maven",
      "pellucid bintray"   at "http://dl.bintray.com/pellucid/maven",
      "justwrote"          at "http://repo.justwrote.it/releases/",
      Resolver.bintrayRepo("kwark", "maven") // Slick with deadlock patch
    ),
    libraryDependencies ++= {
      import Dependencies._
      akka ++ http ++ auth ++ db ++ slick ++ json4s ++ fasterxml ++ apis ++ logging ++ test ++ misc ++ kafka
    },
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.3"),
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
    initialCommands in console := fromFile("project/console_init").getLines.mkString("\n"),
    initialCommands in (Compile, consoleQuick) := "",
    writeVersion <<= sh.toTask(fromFile("project/write_version").getLines.mkString),
    unmanagedResources in Compile += file("version"),
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
    logBuffered in ET   := false,
    test in assembly := {},
    addCommandAlias("all", "; clean; seeder/clean; it:compile; seeder/compile; test; seeder/assembly"),
    scalafmtConfig := Some(file(".scalafmt")),
    reformatOnCompileWithItSettings, // scalafmt
    Revolver.settings,
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
  .dependsOn(phoenixScala)
  .settings(
    commonSettings,
    libraryDependencies ++= Dependencies.gatling,
    cleanFiles <+= baseDirectory(_ / "results"),
    scalafmtConfig := Some(file(".scalafmt")),
    reformatOnCompileSettings, // scalafmt,
    Revolver.settings,
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
      val phoenixClasses = (crossTarget in compile in phoenixScala).value.getAbsolutePath
      (fullClasspath in assembly).value.filterNot(_.data.getAbsolutePath.startsWith(phoenixClasses))
    },
    assemblyExcludedJars in assembly := (fullClasspath in assembly in phoenixScala).value
  )

fullAssembly <<= Def.task().dependsOn(writeVersion in phoenixScala, assembly in phoenixScala, assembly in seeder)

// Injected seeds
val seedCommand = " utils.seeds.Seeds seed --seedAdmins"
seed     := (runMain in Compile in seeder).partialInput(seedCommand).evaluated
seedDemo := (runMain in Compile in seeder).partialInput(s"$seedCommand --seedDemo 1").evaluated

// Gatling seeds
seedOneshot    := (runMain in Compile in seeder).partialInput(" gatling.seeds.OneshotSeeds").evaluated
seedContinuous := (runMain in Compile in seeder).partialInput(" gatling.seeds.ContinuousSeeds").evaluated

// Scalafmt
scalafmtAll <<= Def.task().dependsOn(scalafmt in Compile in phoenixScala,
                                     scalafmt in Test    in phoenixScala,
                                     scalafmt in IT      in phoenixScala,
                                     scalafmt in ET      in phoenixScala,
                                     scalafmt in Compile in seeder)

scalafmtTestAll <<= Def.task().dependsOn(scalafmtTest in Compile in phoenixScala,
                                         scalafmtTest in Test    in phoenixScala,
                                         scalafmtTest in IT      in phoenixScala,
                                         scalafmtTest in ET      in phoenixScala,
                                         scalafmtTest in Compile in seeder)

// Test
test <<= Def.sequential(compile in Test, compile in IT, compile in ET,
                        test    in Test, test    in IT, test    in ET)
