lazy val catsVersion      = "0.9.0"
lazy val akkaHttpVersion  = "10.0.7"
lazy val akkaVersion      = "2.5.2"
lazy val finchVersion     = "0.16.0-RC1"
lazy val sparkVersion     = "2.1.1"
lazy val elastic4sVersion = "5.4.5"

lazy val root = (project in file(".")).settings(
  inThisBuild(
    List(
      organization := "com.example",
      scalaVersion := "2.11.11"
    )),
  name := "anthill-scala",
  libraryDependencies ++= Seq(
    "com.typesafe.akka"      %% "akka-http"            % akkaHttpVersion,
    "com.typesafe.akka"      %% "akka-http-xml"        % akkaHttpVersion,
    "com.typesafe.akka"      %% "akka-stream"          % akkaVersion,
    "com.typesafe.akka"      %% "akka-http-spray-json" % akkaHttpVersion,
    "com.typesafe.akka"      %% "akka-http-testkit"    % akkaHttpVersion % Test,
    "org.scalatest"          %% "scalatest"            % "3.0.1" % Test,
    "org.typelevel"          %% "cats"                 % catsVersion,
    "org.neo4j.driver"       % "neo4j-java-driver"     % "1.2.1",
    "com.github.finagle"     %% "finch-core"           % finchVersion,
    "com.github.finagle"     %% "finch-circe"          % finchVersion,
    "io.circe"               %% "circe-generic"        % "0.8.0",
    "com.chuusai"            %% "shapeless"            % "2.3.2",
    "org.apache.spark"       %% "spark-core"           % sparkVersion % "provided",
    "org.apache.spark"       %% "spark-mllib"          % sparkVersion % "provided",
    "com.sksamuel.elastic4s" %% "elastic4s-core"       % elastic4sVersion,
    "com.sksamuel.elastic4s" %% "elastic4s-tcp"        % elastic4sVersion
  )
)
