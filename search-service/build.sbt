name := "search-service"

version := "0.1-SNAPSHOT"

lazy val core = (project in file("core"))
  .settings(Settings.common)
  .settings(
    libraryDependencies ++= Dependencies.core ++ Dependencies.es
  )

lazy val akka = (project in file("akka"))
  .settings(Settings.common)
  .settings(
    libraryDependencies ++= Dependencies.core ++ Dependencies.akkaHttp
  )
  .dependsOn(core)

lazy val finch = (project in file("finch"))
  .settings(Settings.common)
  .settings(
    libraryDependencies ++= Dependencies.core ++ Dependencies.finch
  )
  .dependsOn(core)

lazy val http4s = (project in file("http4s"))
  .settings(Settings.common)
  .settings(
    libraryDependencies ++= Dependencies.core ++ Dependencies.http4s
  )
  .dependsOn(core)
