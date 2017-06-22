import sbtassembly.{MergeStrategy, PathList}

lazy val core = (project in file("core"))
  .settings(Settings.common)
  .settings(
    libraryDependencies ++= Dependencies.core ++ Dependencies.es ++ Dependencies.circe ++ Dependencies.monix ++ Dependencies.test.core
  )

lazy val finch = (project in file("finch"))
  .settings(Settings.common)
  .settings(
    libraryDependencies ++= Dependencies.finch ++ Dependencies.circe ++ Dependencies.jwt ++ Dependencies.monix
  )

lazy val api = (project in file("api"))
  .settings(Settings.common)
  .settings(Settings.deploy)
  .settings(
    libraryDependencies ++= Dependencies.finch
  )
  .settings(
    Settings.appName := "agni",
    mainClass in assembly := Some("foxcomm.agni.api.Api"),
    assemblyJarName in assembly := s"${Settings.appName.value}.jar",
    assemblyMergeStrategy in assembly := {
      case PathList("BUILD")                                    ⇒ MergeStrategy.discard
      case PathList("META-INF", "io.netty.versions.properties") ⇒ MergeStrategy.discard
      case x                                                    ⇒ (assemblyMergeStrategy in assembly).value.apply(x)
    }
  )
  .dependsOn(core, finch)
  .enablePlugins(AssemblyPlugin, DockerPlugin)
