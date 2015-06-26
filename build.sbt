name := "phoenix-scala"

version := "1.0"

scalaVersion := "2.11.7"

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

mainClass in Compile := Some("Main")

resolvers ++= Seq(
  "hseeberger at bintray" at "http://dl.bintray.com/hseeberger/maven",
  "Pellucid Bintray" at "http://dl.bintray.com/pellucid/maven"
)

libraryDependencies ++= {
  val akkaV       = "2.3.11"
  val akkaStreamV = "1.0-RC2"
  val akkaHttpV   = "1.0-RC4"
  val scalaTestV  = "2.2.5"
  Seq(
    "com.wix"           %% "accord-core"                          % "0.4.2",
    "com.typesafe.akka"      %% "akka-slf4j"          % akkaV,
    "com.typesafe.akka" %% "akka-actor"                           % akkaV,
    "com.typesafe.akka" %% "akka-stream-experimental"             % akkaHttpV,
    "com.typesafe.akka" %% "akka-http-experimental"          % akkaHttpV,
    "org.scalatest"     %% "scalatest"                            % scalaTestV % "test",
    "com.typesafe.slick" %% "slick" % "3.0.0",
    "org.slf4j"          % "slf4j-api"       % "1.7.7",
    "ch.qos.logback"     % "logback-core"    % "1.1.3",
    "ch.qos.logback"     % "logback-classic" % "1.1.3",
    "org.postgresql"    % "postgresql" % "9.3-1100-jdbc41",
    "org.json4s"         %% "json4s-jackson" % "3.2.11",
    "org.scalactic"     %% "scalactic"                            % "2.2.5",
    "org.flywaydb"      %  "flyway-core"      % "3.2.1",
    "com.stripe"        %  "stripe-java"    %  "1.31.0",

    "joda-time"            % "joda-time"          % "2.7",
    "org.joda"             %  "joda-convert"      % "1.7",
    "com.github.tototoshi" %% "slick-joda-mapper" % "2.0.0",
    "com.pellucid" %% "sealerate" % "0.0.3",
    "de.heikoseeberger" %% "akka-http-json4s" % "0.9.1",

    "com.github.julien-truffaut"  %%  "monocle-core"    % "1.1.1",
    "com.github.julien-truffaut"  %%  "monocle-generic" % "1.1.1",
    "com.github.julien-truffaut"  %%  "monocle-macro"   % "1.1.1",

    "com.zaxxer" % "HikariCP" % "2.3.8"
  )
}

lazy val seed = inputKey[Unit]("Resets and seeds the database")

seed := { (runMain in Compile).fullInput(" utils.Seeds").evaluated }

scalaSource in Compile <<= (baseDirectory in Compile)(_ / "app")

scalaSource in Test <<= (baseDirectory in Test)(_ / "test")

resourceDirectory in Compile := baseDirectory.value / "resources"

resourceDirectory in Test := baseDirectory.value / "test" / "resources"

Revolver.settings

parallelExecution in Test := false

fork in Test := true

// add ms report for every test
testOptions in Test += Tests.Argument("-oD")

javaOptions in Test ++= Seq("-Xmx2G", "-XX:+UseConcMarkSweepGC")


