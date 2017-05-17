import Configurations._
import org.scalafmt.sbt.ScalaFmtPlugin
import org.scalafmt.sbt.ScalaFmtPlugin.autoImport._
import sbt.Keys._
import sbt._
import spray.revolver.RevolverPlugin.Revolver

object Settings {

  lazy val commonSettings: Seq[Setting[_]] = Seq(
    version := "1.0",
    scalaVersion := Versions.scala,
    updateOptions := updateOptions.value.withCachedResolution(true),
    excludeDependencies += "org.slf4j.slf4j-log4j12",
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.3"),
    scalacOptions ++= List(
      "-encoding",
      "UTF-8",
      "-target:jvm-1.8",
      "-feature",
      "-unchecked",
      "-deprecation",
      "-Xfatal-warnings",
      "-language:higherKinds",
      "-language:existentials",
      "-Ypartial-unification",
      "-Ywarn-numeric-widen",
      "-Ywarn-nullary-override",
      "-Ywarn-nullary-unit",
      "-Ywarn-infer-any"
    ),
    // Work around SBT warning for multiple dependencies
    dependencyOverrides += "org.scala-lang" % "scala-library" % scalaVersion.value,
    dependencyOverrides ++= Dependencies.slick.toSet,
    dependencyOverrides ++= Dependencies.json4s.toSet,
    ivyScala := ivyScala.value.map(_.copy(overrideScalaVersion = true)),
    resolvers ++= Seq(
      "hseeberger bintray" at "http://dl.bintray.com/hseeberger/maven",
      "pellucid bintray"   at "http://dl.bintray.com/pellucid/maven",
      "justwrote"          at "http://repo.justwrote.it/releases/",
      "confluent"          at "http://packages.confluent.io/maven/",
      Resolver.bintrayRepo("kwark", "maven") // Slick with deadlock patch
    )
  ) ++ scalafmtSettings ++ sourceLocationSettings ++ Revolver.settings

  lazy val scalafmtSettings: Seq[Setting[_]] =
    reformatOnCompileSettings :+ (scalafmtConfig := Some(file(".scalafmt")))

  // Let's keep project sources in `app` directory instead of `src/scala/main`
  // I also prefer `app` over `src` because sources end up on top of list in the project tree view! -- Anna
  lazy val sourceLocationSettings: Seq[Setting[_]] = Seq(
    scalaSource in Compile       := baseDirectory.value / "app",
    resourceDirectory in Compile := baseDirectory.value / "resources"
  )

  lazy val sharedItSettings: Seq[Setting[_]] =
    Defaults.testTasks ++ Defaults.itSettings ++ ScalaFmtPlugin.configScalafmtSettings

  lazy val itSettings: Seq[Setting[_]] = inConfig(IT)(sharedItSettings) ++ Seq(
      testOptions in IT := (testOptions in Test).value :+ Tests.Argument("-l", "tags.External")
    )

  lazy val etSettings: Seq[Setting[_]] = inConfig(ET)(sharedItSettings) ++ Seq(
      testOptions in ET := (testOptions in Test).value :+ Tests.Argument("-n", "tags.External")
    )

}
