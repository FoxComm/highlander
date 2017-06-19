lazy val catsVersion     = "0.9.0"
lazy val akkaHttpVersion = "10.0.7"
lazy val akkaVersion     = "2.5.2"

lazy val root = (project in file(".")).settings(
  inThisBuild(
    List(
      organization := "com.example",
      scalaVersion := "2.12.2"
    )),
  name := "anthill-scala",
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-xml"        % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-stream"          % akkaVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-testkit"    % akkaHttpVersion % Test,
    "org.scalatest"     %% "scalatest"            % "3.0.1" % Test,
    "org.typelevel"     %% "cats"                 % catsVersion,
    "org.neo4j.driver"  % "neo4j-java-driver"     % "1.2.1"
  )
)
