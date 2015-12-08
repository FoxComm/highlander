package responses

import java.time.Instant
import scala.concurrent.ExecutionContext

import models.activity.Activity
import models.activity.ActivityContext
import models.activity.Aliases.ActivityType
import models.activity.Aliases.Json

import services.NotFoundFailure404
import utils.Slick.DbResult
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._


object ActivityResponse {
  final case class Root(
    id: Int, 
    kind: ActivityType,
    data: Json,
    context: ActivityContext,
    createdAt: Instant) extends ResponseItem

    def build(a: Activity) : Root = 
      Root(
        id = a.id,
        kind = a.activityType,
        data = a.data,
        context = a.context,
        createdAt = a.createdAt)

}
