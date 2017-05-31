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
  .settings(
    libraryDependencies ++= Dependencies.akkaHttp
  )
  .dependsOn(core)
  .enablePlugins(ScalafmtPlugin)

lazy val finch = (project in file("finch"))
  .settings(Settings.common)
  .settings(
    libraryDependencies ++= Dependencies.finch
  )
  .dependsOn(core)
  .enablePlugins(ScalafmtPlugin)

lazy val http4s = (project in file("http4s"))
  .settings(Settings.common)
  .settings(
    libraryDependencies ++= Dependencies.http4s
  )
  .dependsOn(core)
  .enablePlugins(ScalafmtPlugin)
