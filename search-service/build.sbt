import sbtassembly.AssemblyKeys.assemblyExcludedJars
import sbtassembly.{MergeStrategy, PathList}

name := "search-service"

version := "0.1-SNAPSHOT"

lazy val core = (project in file("core"))
  .settings(Settings.common)
  .settings(
    libraryDependencies ++= Dependencies.core ++ Dependencies.es ++ Dependencies.json
  )

lazy val api = (project in file("api"))
  .settings(Settings.common)
  .settings(Settings.deploy)
  .settings(
    libraryDependencies ++= Dependencies.http
  )
  .settings(
    mainClass in assembly := Some("foxcomm.search.Main"),
    assemblyJarName in assembly := "search-service.jar",
    assemblyMergeStrategy in assembly := {
      case PathList("BUILD") ⇒ MergeStrategy.discard
      case PathList("META-INF", "io.netty.versions.properties") ⇒ MergeStrategy.discard
      case x => (assemblyMergeStrategy in assembly).value.apply(x)
    }
  )
  .dependsOn(core)
  .enablePlugins(AssemblyPlugin, DockerPlugin)
