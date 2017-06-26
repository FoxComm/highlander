import Configurations._
import sbt.Keys._
import sbt._
import sbtassembly.AssemblyKeys._
import sbtassembly.AssemblyPlugin.autoImport.assemblyMergeStrategy
import sbtassembly.{MergeStrategy, PathList}
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
    dependencyOverrides ++= Dependencies.slick.toSet,
    dependencyOverrides ++= Dependencies.json4s.toSet,
    ivyScala := ivyScala.value.map(_.copy(overrideScalaVersion = true)),
    // Exclude vanilla Scala to avoid assembly collisions with typelevel
    assemblyExcludedJars in assembly := {
      val cp = (fullClasspath in assembly).value
      cp.filter(_.data.getName == "scala-library-" + scalaVersion.value + ".jar")
    },
    test in assembly := {},
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
    resolvers ++= Seq(
      "hseeberger bintray" at "http://dl.bintray.com/hseeberger/maven",
      "pellucid bintray" at "http://dl.bintray.com/pellucid/maven",
      "justwrote" at "http://repo.justwrote.it/releases/",
      "confluent" at "http://packages.confluent.io/maven/",
      Resolver.bintrayRepo("kwark", "maven") // Slick with deadlock patch
    )
  ) ++ sourceLocationSettings ++ Revolver.settings

  // Let's keep project sources in `app` directory instead of `src/scala/main`
  // I also prefer `app` over `src` because sources end up on top of list in the project tree view! -- Anna
  lazy val sourceLocationSettings: Seq[Setting[_]] = Seq(
    scalaSource in Compile := baseDirectory.value / "app",
    resourceDirectory in Compile := baseDirectory.value / "resources"
  )

  lazy val sharedItSettings: Seq[Setting[_]] =
    Defaults.testTasks ++ Defaults.itSettings

  lazy val itSettings: Seq[Setting[_]] = inConfig(IT)(sharedItSettings) ++ Seq(
    testOptions in IT := (testOptions in Test).value :+ Tests.Argument("-l", "tags.External")
  )

  lazy val etSettings: Seq[Setting[_]] = inConfig(ET)(sharedItSettings) ++ Seq(
    testOptions in ET := (testOptions in Test).value :+ Tests.Argument("-n", "tags.External")
  )

}
