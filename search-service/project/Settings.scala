import com.lucidchart.sbt.scalafmt.ScalafmtCorePlugin.autoImport._
import sbt.Keys._
import sbt._
import sbtassembly.AssemblyKeys._
import sbtassembly.AssemblyPlugin.autoImport.assemblyMergeStrategy
import sbtassembly.{MergeStrategy, PathList}
import sbtdocker.DockerKeys._
import sbtdocker._
import sbtdocker.immutable.Dockerfile
import wartremover.{Wart, Warts, wartremoverErrors}

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

    wartremoverErrors in (Compile, compile) ++= Warts.allBut(Wart.ImplicitParameter, Wart.NonUnitStatements, Wart.Nothing, Wart.PublicInference),

    scalafmtVersion := "1.0.0-RC1",
    compile in Compile := (compile in Compile).dependsOn(scalafmt in Compile).value,

    scalaSource in Compile       := baseDirectory.value / "app",
    resourceDirectory in Compile := baseDirectory.value / "resources"
  )

  def deploy: Seq[Def.Setting[_]] = Seq(
    assemblyMergeStrategy in assembly := {
      case PathList("org", "joda", "time", _ @ _ *) ⇒ MergeStrategy.first
      case x => (assemblyMergeStrategy in assembly).value.apply(x)
    },
    test in assembly := {},
    dockerfile in docker := {
      val artifact: File = assembly.value
      val artifactTargetPath = s"/app/${artifact.name}"

      Dockerfile.empty
        .from("openjdk:8-alpine")
        .add(artifact, artifactTargetPath)
        .cmdRaw(s"java -jar $artifactTargetPath")
    } ,
    imageNames in docker := Seq(
      ImageName(s"${sys.props("DOCKER_REPO")}/${(assemblyJarName in assembly).value.stripSuffix(".jar")}:${sys.props("DOCKER_TAG")}")
    )
  )
}
