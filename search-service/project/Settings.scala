import com.lucidchart.sbt.scalafmt.ScalafmtCorePlugin.autoImport.{scalafmtOnCompile, scalafmtVersion}
import sbt.Keys._
import sbt._
import wartremover.WartRemover.autoImport.{Wart, Warts, wartremoverErrors}

object Settings {
  def common: Seq[Def.Setting[_]] = Seq(
    scalaVersion := "2.11.11",
    scalacOptions in Compile ++= Seq(
      "-deprecation",
      "-encoding", "UTF-8",
      "-feature",
      "-unchecked",
      "-Yno-adapted-args",
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen",
      "-Ywarn-unused-import",
      "-Xfatal-warnings",
      "-Xfuture"
    ),

    wartremoverErrors in (Compile, compile) ++= Warts.allBut(Wart.ImplicitParameter, Wart.PublicInference),
    wartremoverErrors in (Compile, test) ++= Warts.allBut(Wart.NonUnitStatements),

    scalafmtVersion := "1.0.0-RC1",
    scalafmtOnCompile := true,

    scalaSource in Compile       := baseDirectory.value / "app",
    resourceDirectory in Compile := baseDirectory.value / "resources"
  )
}
