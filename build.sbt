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
  "-Xfatal-warnings",
  "-language:higherKinds"
)

libraryDependencies ++= {
  val akkaV       = "2.3.10"
  val akkaStreamV = "1.0-RC2"
  val akkaHttpV = "1.0-RC3"
  val scalaTestV  = "2.2.4"
  Seq(
    "com.wix"           %% "accord-core"                          % "0.4.2",
    "com.typesafe.akka" %% "akka-actor"                           % akkaV,
    "com.typesafe.akka" %% "akka-stream-experimental"             % akkaHttpV,
    "com.typesafe.akka" %% "akka-http-experimental"          % akkaHttpV,
    "com.typesafe.akka" %% "akka-http-spray-json-experimental"    % akkaHttpV,
    "org.scalatest"     %% "scalatest"                            % scalaTestV % "test",
    "com.typesafe.slick" %% "slick" % "3.0.0",
    "org.slf4j"          % "slf4j-nop" % "1.6.4",
    "org.postgresql"    % "postgresql" % "9.3-1100-jdbc41",
    "org.json4s"         %% "json4s-jackson" % "3.2.11",
    "org.scalactic"     %% "scalactic"                            % "2.2.4",
    "org.flywaydb"      %  "flyway-core"      % "3.2.1"              % "test",
    "com.stripe"        %  "stripe-java"    %  "1.31.0",
    "com.github.julien-truffaut"  %%  "monocle-core"    % "1.1.1",
    "com.github.julien-truffaut"  %%  "monocle-generic" % "1.1.1",
    "com.github.julien-truffaut"  %%  "monocle-macro"   % "1.1.1"
  )
}

scalaSource in Compile <<= (baseDirectory in Compile)(_ / "app")

scalaSource in Test <<= (baseDirectory in Test)(_ / "test")

resourceDirectory in Compile := baseDirectory.value / "resources"

resourceDirectory in Test := baseDirectory.value / "resources"

Revolver.settings

parallelExecution in Test := false
