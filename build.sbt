name := "phoenix-scala"

version := "1.0"

scalaVersion := "2.11.6"

scalacOptions ++= List(
  "-encoding", "UTF-8",
  "-target:jvm-1.8",
  "-feature",
  "-unchecked",
  "-deprecation",
  "-Xlint",
  "-Xfatal-warnings"
)

libraryDependencies ++= {
  val akkaV       = "2.3.10"
  val akkaStreamV = "1.0-RC2"
  val scalaTestV  = "2.2.4"
  Seq(
    "com.wix"           %% "accord-core"                          % "0.4.2",
    "com.typesafe.akka" %% "akka-actor"                           % akkaV,
    "com.typesafe.akka" %% "akka-stream-experimental"             % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-core-experimental"          % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-scala-experimental"         % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-spray-json-experimental"    % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-testkit-scala-experimental" % akkaStreamV,
    "org.scalatest"     %% "scalatest"                            % scalaTestV % "test",
    "com.typesafe.slick" %% "slick" % "3.0.0",
    "org.slf4j"          % "slf4j-nop" % "1.6.4",
    "org.postgresql"    % "postgresql" % "9.3-1100-jdbc41",
    "org.json4s"         %% "json4s-jackson" % "3.2.11"
  )
}

scalaSource in Compile <<= (baseDirectory in Compile)(_ / "app")

scalaSource in Test <<= (baseDirectory in Test)(_ / "test")

Revolver.settings

