import sbt._

object Configurations {
  // Integration tests
  lazy val IT = config("it") extend Test

  // External tests
  lazy val ET = config("et") extend IT
}
