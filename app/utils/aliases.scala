package utils

object aliases {
  type AC = models.activity.ActivityContext
  type EC = scala.concurrent.ExecutionContext
  type DB = slick.driver.PostgresDriver.api.Database
}
