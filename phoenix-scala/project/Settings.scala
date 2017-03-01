import Configurations._
import org.scalafmt.sbt.ScalaFmtPlugin
import sbt.Keys._
import sbt._

object Settings {

  lazy val commonSettings = Seq(
    version := "1.0",
    scalaVersion := Versions.scala,
    updateOptions := updateOptions.value.withCachedResolution(true),
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
    )
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
