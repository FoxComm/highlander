lazy val commonSettings = Seq(
  version       := "1.0",
  scalaVersion  := "2.11.8",
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
      val akkaV      = "2.4.4"
      val scalaTestV = "2.2.6"
      val json4sV    = "3.3.0"

      Seq(
        // Config
        "com.typesafe"              % "config"                    % "1.3.0",
        // Cache
        "com.github.cb372"          %% "scalacache-lrumap"        % "0.8.1",
        // JSON
        "org.json4s"                %% "json4s-core"              % json4sV,
        "org.json4s"                %% "json4s-jackson"           % json4sV,
        "org.json4s"                %% "json4s-ext"               % json4sV,
        // Search
        "org.apache.kafka"          % "kafka_2.11"                % "0.9.0.1",
        "io.confluent"              % "kafka-avro-serializer"     % "1.0",
        "com.sksamuel.elastic4s"    %% "elastic4s-core"           % "2.3.0",
        // Akka
        "com.typesafe.akka"         %% "akka-slf4j"               % akkaV,
        "com.typesafe.akka"         %% "akka-actor"               % akkaV,
        "com.typesafe.akka"         %% "akka-agent"               % akkaV,
        "com.typesafe.akka"         %% "akka-stream"              % akkaV,
        "com.typesafe.akka"         %% "akka-http-core"           % akkaV,
        "de.heikoseeberger"         %% "akka-http-json4s"         % "1.6.0",
        // Cats
        "org.typelevel"             %% "cats"                     % "0.5.0",
        // Testing
        "org.scalatest"             %% "scalatest"                % scalaTestV % "test"
      )
    },
    (mainClass in Compile) := Some("consumer.Main"),
    assemblyMergeStrategy in assembly := {
        case PathList("org", "joda", xs @ _*) ⇒  MergeStrategy.last
        case x ⇒ { 
            val old = (assemblyMergeStrategy in assembly).value
            old(x)
        }
    }
    //test in assembly := {}
)


lazy val consume = inputKey[Unit]("Runs the Kafka Consumers")
consume := { (runMain in Compile).partialInput(" consumer.Main").evaluated }
