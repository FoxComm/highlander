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
      val akkaV      = "2.3.11"
      val akkaHttpV  = "1.0"
      val scalaTestV = "2.2.5"
      val monocleV   = "1.1.1"
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
        "com.sksamuel.elastic4s"    %% "elastic4s-core"       % "1.7.4",
        // Akka
        "com.typesafe.akka"    %% "akka-slf4j"               % akkaV,
        "com.typesafe.akka"    %% "akka-actor"               % akkaV,
        "com.typesafe.akka"    %% "akka-agent"               % akkaV,
        "com.typesafe.akka"    %% "akka-stream-experimental" % akkaHttpV,
        "com.typesafe.akka"    %% "akka-http-experimental"   % akkaHttpV,
        "de.heikoseeberger"    %% "akka-http-json4s"         % "1.0.0",
        // Cats
        "org.spire-math"       %% "cats"                      % "0.3.0",
        // Testing
        "org.scalatest"        %% "scalatest"                 % scalaTestV % "test"    
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
