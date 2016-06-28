import sbt._

object Dependencies {

  val akkaV = "2.4.7"
  val akka = Seq(
      "com.typesafe.akka" %% "akka-slf4j"          % akkaV,
      "com.typesafe.akka" %% "akka-actor"          % akkaV,
      "com.typesafe.akka" %% "akka-agent"          % akkaV,
      "com.typesafe.akka" %% "akka-stream"         % akkaV,
      "com.typesafe.akka" %% "akka-http-core"      % akkaV,
      "de.heikoseeberger" %% "akka-sse"            % "1.8.1",
      "com.typesafe.akka" %% "akka-testkit"        % akkaV % "test,it",
      "com.typesafe.akka" %% "akka-stream-testkit" % akkaV % "test,it"
  )

  val slickV = "3.1.1"
  val slick = Seq(
      "com.typesafe.slick" %% "slick"          % slickV,
      "com.typesafe.slick" %% "slick-hikaricp" % slickV
  )

  val json4sV = "3.4.0"
  val json4s = Seq(
      "org.json4s"        %% "json4s-core"      % json4sV,
      "org.json4s"        %% "json4s-jackson"   % json4sV,
      "org.json4s"        %% "json4s-ext"       % json4sV,
      "de.heikoseeberger" %% "akka-http-json4s" % "1.7.0"
  )
}
