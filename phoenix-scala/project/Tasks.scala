import sbt._

object Tasks {

  lazy val scalafmtAll     = taskKey[Unit]("scalafmt all the things")
  lazy val scalafmtTestAll = taskKey[Unit]("scalafmtTest all the things")

  lazy val fullAssembly = taskKey[Unit]("Assembly all the things")

  lazy val writeVersion = taskKey[Seq[String]]("Write project version data to version file")

  lazy val seed           = inputKey[Unit]("Reset and seed the database (admins only)")
  lazy val seedDemo       = inputKey[Unit]("Reset and seed the database (admins and test data)")
  lazy val seedOneshot    = inputKey[Unit]("Run oneshot gatling seeds")
  lazy val seedContinuous = inputKey[Unit]("Run continuous gatling seeds")

  lazy val testSimulations = taskKey[Unit]("Run gatling test simulations")
}
