import sbt._

object Tasks {

  lazy val scalafmtAll     = taskKey[Unit]("scalafmt all the things")
  lazy val scalafmtTestAll = taskKey[Unit]("scalafmtTest all the things")

  lazy val fullAssembly = taskKey[Unit]("Assembly all the things")

  lazy val writeVersion = taskKey[Seq[String]]("Write project version data to version file")

  lazy val seed           = inputKey[Unit]("Resets and seeds the database")
  lazy val seedOneshot    = inputKey[Unit]("Run oneshot gatling seeds")
  lazy val seedContinuous = inputKey[Unit]("Run continuous gatling seeds")

}
