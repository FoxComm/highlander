import sbtassembly.AssemblyKeys.assemblyExcludedJars
import sbtassembly.{MergeStrategy, PathList}

name := "search-service"

version := "0.1-SNAPSHOT"

lazy val core = (project in file("core"))
  .settings(Settings.common)
  .settings(
    libraryDependencies ++= Dependencies.core ++ Dependencies.es ++ Dependencies.json
  )
  .enablePlugins(ScalafmtPlugin)

lazy val akka = (project in file("akka"))
  .settings(Settings.common)
  .settings(Settings.deploy)
  .settings(
    libraryDependencies ++= Dependencies.akkaHttp
  )
  .settings (
    mainClass in assembly := Some("foxcomm.search.AkkaAPI"),
    assemblyJarName in assembly := "search-service-akka.jar"
  )
  .dependsOn(core)
  .enablePlugins(AssemblyPlugin, DockerPlugin, ScalafmtPlugin)

lazy val finch = (project in file("finch"))
  .settings(Settings.common)
  .settings(Settings.deploy)
  .settings(
    libraryDependencies ++= Dependencies.finch
  )
  .settings(
    mainClass in assembly := Some("foxcomm.search.FinchAPI"),
    assemblyJarName in assembly := "search-service-finch.jar",
    assemblyMergeStrategy in assembly := {
      case PathList("BUILD") ⇒ MergeStrategy.discard
      case PathList("META-INF", "io.netty.versions.properties") ⇒ MergeStrategy.discard
      case x => (assemblyMergeStrategy in assembly).value.apply(x)
    }
  )
  .dependsOn(core)
  .enablePlugins(AssemblyPlugin, DockerPlugin, ScalafmtPlugin)

lazy val http4s = (project in file("http4s"))
  .settings(Settings.common)
  .settings(Settings.deploy)
  .settings(
    libraryDependencies ++= Dependencies.http4s
  )
  .settings(
    assemblyExcludedJars in assembly := {
      val cp = (fullClasspath in assembly).value
      cp.filter(_.data.getName == "scala-library-" + scalaVersion.value + ".jar")
    },
    mainClass in assembly := Some("foxcomm.search.Http4sAPI"),
    assemblyJarName in assembly := "search-service-http4s.jar"
  )
  .dependsOn(core)
  .enablePlugins(AssemblyPlugin, DockerPlugin, ScalafmtPlugin)
