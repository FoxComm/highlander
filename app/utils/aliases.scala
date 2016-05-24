package utils

object aliases {
  type AC  = models.activity.ActivityContext
  type EC  = scala.concurrent.ExecutionContext
  type ES  = utils.ElasticsearchApi
  type DB  = slick.driver.PostgresDriver.api.Database
  type Mat = akka.stream.Materializer
}
