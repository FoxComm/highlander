import scala.io.Source.fromFile

import Configurations._
import Settings._
import Tasks._
import sbtassembly.AssemblyKeys

scalaVersion in ThisBuild := Versions.scala

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
      akka ++ http ++ auth ++ db ++ slick ++ json4s ++ fasterxml ++ apis ++ logging ++ test ++ misc
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
    initialCommands in console := fromFile("project/console_init").getLines.mkString("\n"),
    initialCommands in (Compile, consoleQuick) := "",
    writeVersion <<= sh.toTask(fromFile("project/write_version").getLines.mkString),
    unmanagedResources in Compile += file("version"),
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
    addCommandAlias("assembly", "fullAssembly"),
    addCommandAlias("all", "; clean; gatling/clean; it:compile; gatling/compile; test; gatling/assembly"),
    scalafmtConfig := Some(file(".scalafmt")),
    reformatOnCompileWithItSettings // scalafmt
  )

lazy val gatling = (project in file("gatling"))
  .dependsOn(phoenixScala)
  .settings(
    commonSettings,
    libraryDependencies ++= Dependencies.gatling,
    classDirectory in Compile := baseDirectory.value / "../gatling-classes",
    cleanFiles <+= baseDirectory(_ / "../gatling-classes"),
    cleanFiles <+= baseDirectory(_ / "../gatling-results"),
    assemblyJarName := (AssemblyKeys.assemblyJarName in assembly in phoenixScala).value,
    scalafmtConfig := Some(file(".scalafmt")),
    reformatOnCompileSettings, // scalafmt,
    Revolver.settings,
    assemblyMergeStrategy in assembly := {
      case PathList("org", "joda", "time", xs @ _ *) ⇒
        MergeStrategy.first
      case PathList("io", "netty", xs @ _ *) ⇒
        MergeStrategy.first
      case PathList("META-INF", "io.netty.versions.properties") ⇒
        MergeStrategy.first
      case PathList("META-INF", "native", "libnetty-transport-native-epoll.so") ⇒
        MergeStrategy.first
      case x ⇒
        (assemblyMergeStrategy in assembly).value.apply(x)
    }
  )

fullAssembly <<= Def.task().dependsOn(writeVersion in phoenixScala, assembly in gatling)

// Injected seeds
val seedCommand = " utils.seeds.Seeds seed --seedAdmins"
seed      := (runMain in Compile in phoenixScala).partialInput(seedCommand).evaluated
seedStage := (runMain in Compile in phoenixScala).partialInput(s"$seedCommand --seedStage").evaluated
seedDemo  := (runMain in Compile in phoenixScala).partialInput(s"$seedCommand --seedDemo 1").evaluated

// Gatling seeds
seedOneshot    := (runMain in Compile in gatling).partialInput(" seeds.OneshotSeeds").evaluated
seedContinuous := (runMain in Compile in gatling).partialInput(" seeds.ContinuousSeeds").evaluated

// Scalafmt
scalafmtAll <<= Def.task().dependsOn(scalafmt in Compile in phoenixScala,
                                     scalafmt in Test    in phoenixScala,
                                     scalafmt in IT      in phoenixScala,
                                     scalafmt in ET      in phoenixScala,
                                     scalafmt in Compile in gatling)

scalafmtTestAll <<= Def.task().dependsOn(scalafmtTest in Compile in phoenixScala,
                                         scalafmtTest in Test    in phoenixScala,
                                         scalafmtTest in IT      in phoenixScala,
                                         scalafmtTest in ET      in phoenixScala,
                                         scalafmtTest in Compile in gatling)

// Test
test <<= Def.sequential(compile in Test, compile in IT, compile in ET,
                        test    in Test, test    in IT, test    in ET)
