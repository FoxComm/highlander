lazy val commonSettings = Seq(
  version       := "1.0",
  scalaVersion  := "2.12.1",
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


lazy val greenRiver = (project in file(".")).
  settings(commonSettings).
  settings(
    name      := "green-river",
    version   := "1.0",
    resolvers ++= Seq(
      "confluent" at "http://packages.confluent.io/maven"
    ),
    libraryDependencies ++= {
      val akkaV      = "2.4.16"
      val akkaHttpV  = "10.0.0"
      val scalaTestV = "3.0.1"
      val json4sV    = "3.5.0"

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
        "com.sksamuel.elastic4s"    %% "elastic4s-core"           % "5.1.5",
        // Akka
        "com.typesafe.akka"         %% "akka-slf4j"               % akkaV,
        "com.typesafe.akka"         %% "akka-actor"               % akkaV,
        "com.typesafe.akka"         %% "akka-agent"               % akkaV,
        "com.typesafe.akka"         %% "akka-stream"              % akkaV,
        "com.typesafe.akka"         %% "akka-http-core"           % akkaHttpV,
        "de.heikoseeberger"         %% "akka-http-json4s"         % "1.11.0",
        // Cats
        "org.typelevel"             %% "cats"                     % "0.9.0",
        // Testing
        "org.scalatest"             %% "scalatest"                % scalaTestV % "test"
      )
    },
    (mainClass in Compile) := Some("consumer.Main"),
    (mainClass in reStart) := Some("consumer.Main"),
    cleanupCommands in console := """system.terminate""",
    initialCommands in console :=
      """
        |import scala.concurrent.ExecutionContext.Implicits.global
        |import scala.concurrent.Future
        |import scala.concurrent.duration._
        |import scalacache.ScalaCache
        |import scalacache.lrumap.LruMapCache
        |import akka.actor.ActorSystem
        |import akka.stream.ActorMaterializer
        |import cats.implicits._
        |import consumer.MainConfig
        |import consumer.utils.{Phoenix, PhoenixConnectionInfo}
        |implicit val system = ActorSystem("system")
        |implicit val materializer = ActorMaterializer()
        |import akka.http.scaladsl.settings.ConnectionPoolSettings
        |implicit val connectionPoolSettings = ConnectionPoolSettings.default(implicitly[ActorSystem])
        |implicit val scalaCache = ScalaCache(LruMapCache(1))

        |val config = MainConfig.loadFromConfig
        |val connInfo = PhoenixConnectionInfo(config.phoenixUri, config.phoenixUser, config.phoenixPass, config
        |.phoenixOrg)
        |val conn = Phoenix(connInfo)
      """.stripMargin,
    assemblyMergeStrategy in assembly := {
        case PathList("org", "joda", xs @ _*) ⇒  MergeStrategy.last
        case x ⇒ {
            val old = (assemblyMergeStrategy in assembly).value
            old(x)
        }
    },
    scalafmtConfig := Some(file(".scalafmt")),
    reformatOnCompileSettings // scalafmt
    //test in assembly := {}
)


lazy val consume = inputKey[Unit]("Runs the Kafka Consumers")
consume := { (runMain in Compile).partialInput(" consumer.Main").evaluated }

lazy val createMappings = inputKey[Unit]("Runs Mapping Creation in ES")
createMappings := { (runMain in Compile).partialInput(" consumer.Main").evaluated }
