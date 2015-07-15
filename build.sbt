lazy val commonSettings = Seq(
  version      := "1.0",
  scalaVersion := "2.11.7",
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
)

lazy val phoenixScala = (project in file(".")).
  settings(commonSettings: _*).
  configs(IT).
  settings(inConfig(IT)(Defaults.testSettings): _*).
  settings(
    name      := "phoenix-scala",
    version   := "1.0",
    /** Work around SBT warning for multiple dependencies */
    dependencyOverrides += "org.scala-lang" % "scala-library" % scalaVersion.value,
    ivyScala            := ivyScala.value.map(_.copy(overrideScalaVersion = true)),
    resolvers ++= Seq(
      "hseeberger bintray" at "http://dl.bintray.com/hseeberger/maven",
      "pellucid bintray"   at "http://dl.bintray.com/pellucid/maven"
    ),
    libraryDependencies ++= {
      val akkaV       = "2.3.11"
      val akkaHttpV   = "1.0"
      val scalaTestV  = "2.2.5"
      val monocleV    = "1.1.1"

      Seq(
        // Akka
        "com.typesafe.akka"    %% "akka-slf4j"               % akkaV,
        "com.typesafe.akka"    %% "akka-actor"               % akkaV,
        "com.typesafe.akka"    %% "akka-stream-experimental" % akkaHttpV,
        "com.typesafe.akka"    %% "akka-http-experimental"   % akkaHttpV,
        // JSON
        "org.json4s"           %% "json4s-jackson"           % "3.2.11",
        "de.heikoseeberger"    %% "akka-http-json4s"         % "0.9.1",
        // Database
        "com.typesafe.slick"   %% "slick"                    % "3.0.0",
        "com.zaxxer"           %  "HikariCP"                 % "2.3.8",
        "com.github.tototoshi" %% "slick-joda-mapper"        % "2.0.0",
        "org.postgresql"       %  "postgresql"               % "9.4-1201-jdbc41",
        "org.flywaydb"         %  "flyway-core"              % "3.2.1",
        // Validations
        "com.wix"              %% "accord-core"              % "0.4.2",
        "org.scalactic"        %% "scalactic"                % "2.2.5",
        // Logging
        "ch.qos.logback"       %  "logback-core"              % "1.1.3",
        "ch.qos.logback"       %  "logback-classic"           % "1.1.3",
        // Other
        "com.stripe"           %  "stripe-java"               % "1.31.0",
        "org.slf4j"            %  "slf4j-api"                 % "1.7.12",
        "joda-time"            %  "joda-time"                 % "2.8.1",
        "org.joda"             %  "joda-convert"              % "1.7",
        "org.joda"             %  "joda-money"                % "0.10.0",
        "com.pellucid"         %% "sealerate"                 % "0.0.3",
        "com.github.julien-truffaut" %% "monocle-core"        % monocleV,
        "com.github.julien-truffaut" %% "monocle-generic"     % monocleV,
        "com.github.julien-truffaut" %% "monocle-macro"       % monocleV,
        // Testing
        "org.scalatest"        %% "scalatest"                 % scalaTestV % "test"
      )
    },
    scalaSource in Compile <<= (baseDirectory in Compile)(_ / "app"),
    scalaSource in Test <<= (baseDirectory in Test)(_ / "test" / "unit"),
    scalaSource in IT   <<= (baseDirectory in Test)(_ / "test" / "integration"),
    resourceDirectory in Compile := baseDirectory.value / "resources",
    resourceDirectory in Test := baseDirectory.value / "test" / "resources",
    resourceDirectory in IT   := baseDirectory.value / "test" / "resources",
    Revolver.settings,
    (mainClass in Compile) := Some("Main"),
    // add ms report for every test
    testOptions in Test += Tests.Argument("-oD"),
    javaOptions in Test ++= Seq("-Xmx2G", "-XX:+UseConcMarkSweepGC"),
    parallelExecution in Test := true,
    parallelExecution in IT   := false,
    fork in Test := false,
    fork in IT   := true, /** FIXME: We couldn’t run ITs in parallel if we fork */
    test <<= Def.task {
      /** We need to do nothing here. Unit and ITs will run in parallel
        * and this task will fail if any of those fail. */
      ()
    }.dependsOn(test in Test, test in IT)
)

lazy val IT = config("it") extend Test

lazy val seed = inputKey[Unit]("Resets and seeds the database")
seed := { (runMain in Compile).fullInput(" utils.Seeds").evaluated }
