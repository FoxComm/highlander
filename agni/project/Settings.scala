import sbt.Keys._
import sbt._
import sbtassembly.AssemblyKeys._
import sbtassembly.AssemblyPlugin.autoImport.assemblyMergeStrategy
import sbtassembly.{MergeStrategy, PathList}
import sbtdocker.DockerKeys._
import sbtdocker._
import sbtdocker.immutable.Dockerfile
import wartremover.{wartremoverErrors, Wart, Warts}

object Settings {
  lazy val appName: TaskKey[String] = taskKey[String]("Name for a application")

  def common: Seq[Def.Setting[_]] = Seq(
    scalaVersion := "2.11.11",
    scalacOptions in Compile ++= Seq(
      "-deprecation",
      "-encoding",
      "UTF-8",
      "-feature",
      "-unchecked",
      "-Yno-adapted-args",
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen",
      "-Ywarn-unused-import",
      "-Xfatal-warnings",
      "-Xfuture"
    ),
    wartremoverErrors in (Compile, compile) ++= Warts.allBut(Wart.Any,
                                                             Wart.ImplicitParameter,
                                                             Wart.Nothing,
                                                             Wart.PublicInference),
    scalaSource in Compile := baseDirectory.value / "app",
    resourceDirectory in Compile := baseDirectory.value / "resources",
    scalaSource in Test := baseDirectory.value / "test",
    resourceDirectory in Test := baseDirectory.value / "test" / "resources"
  )

  def deploy: Seq[Def.Setting[_]] = Seq(
    assemblyMergeStrategy in assembly := {
      case PathList("org", "joda", "time", _ @_ *) ⇒ MergeStrategy.first
      case x                                       ⇒ (assemblyMergeStrategy in assembly).value.apply(x)
    },
    test in assembly := {},
    dockerfile in docker := {
      val artifact: File     = assembly.value
      val artifactTargetPath = s"/app/${artifact.name}"

      Dockerfile.empty
        .from("openjdk:8-alpine")
        .add(artifact, artifactTargetPath)
        .cmdRaw(s"java $$JAVA_OPTS -jar $artifactTargetPath 2>&1 | tee /logs/${appName.value}.log")
    },
    imageNames in docker := Seq(
      ImageName(
        s"${sys.props("DOCKER_REPO")}/${appName.value}:${sys.props("DOCKER_TAG")}")
    )
  )
}
