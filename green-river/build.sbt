scalafmtConfig in ThisBuild := Some(file(".scalafmt.conf"))

version in ThisBuild := "1.0"

scalaVersion in ThisBuild := "2.11.8"

lazy val commonSettings = Seq(
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
    reformatOnCompileSettings // scalafmt
    //test in assembly := {}
)


lazy val consume = inputKey[Unit]("Runs the Kafka Consumers")
consume := { (runMain in Compile).partialInput(" consumer.Main").evaluated }

lazy val createMappings = inputKey[Unit]("Runs Mapping Creation in ES")
createMappings := { (runMain in Compile).partialInput(" consumer.Main").evaluated }
