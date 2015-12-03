lazy val commonSettings = Seq(
  version       := "1.0",
  scalaVersion  := "2.11.7",
  updateOptions := updateOptions.value.withCachedResolution(true),
  scalacOptions ++= List(
    "-encoding", "UTF-8",
    "-target:jvm-1.8",
    "-feature",
    "-unchecked",
    "-deprecation",
    "-Xlint",
    "-Xfatal-warnings",
    "-language:higherKinds",
    "-language:existentials",
    "-Ywarn-numeric-widen",
    "-Ywarn-nullary-override",
    "-Ywarn-nullary-unit",
    "-Ywarn-infer-any"
  )
)

lazy val phoenixScala = (project in file(".")).
  settings(commonSettings).
  settings(
    name      := "green-river",
    version   := "1.0",
    resolvers ++= Seq(
      "confluent" at "http://packages.confluent.io/maven"
    ),
    libraryDependencies ++= {
      val json4sVersion = "3.3.0.RC3"

      Seq(
        // Config
        "com.typesafe"              % "config"                % "1.3.0",
        // JSON
        "org.json4s"                %% "json4s-core"          % json4sVersion,
        "org.json4s"                %% "json4s-jackson"       % json4sVersion,
        "org.json4s"                %% "json4s-ext"           % json4sVersion,        
        // Search
        "org.apache.kafka"          % "kafka_2.11"            % "0.9.0.0",
        "io.confluent"              % "kafka-avro-serializer" % "1.0",
        "com.sksamuel.elastic4s"    %% "elastic4s-core"       % "1.7.4"
      )
    },
    (mainClass in Compile) := Some("consumer.Main")
)

lazy val consume = inputKey[Unit]("Runs the Kafka consumer")
consume := { (runMain in Compile).partialInput(" consumer.Main").evaluated }