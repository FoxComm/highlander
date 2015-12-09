package responses

import java.time.Instant
import scala.concurrent.ExecutionContext

import models.activity.ActivityContext
import models.activity.Aliases.ActivityType
import models.activity.Aliases.Json
import models.activity.Connection
import models.activity.Dimension
import models.activity.{Activity ⇒ ActivityModel}

import services.NotFoundFailure404
import utils.Slick.DbResult
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._

object ActivityConnectionResponse {

  final case class Root(
    id: Int, 
    dimension: String,
    objectId: Int,
    trailId: Int,
    activityId: Int,
    previousId: Option[Int],
    nextId: Option[Int],
    data: Option[Json],
    connectedBy: ActivityContext,
    createdAt: Instant) extends ResponseItem

    def build(objectId: Int, d: Dimension , c: Connection) : Root =  {
      Root(
        id = c.id,
        dimension = d.name,
        objectId = objectId,
        trailId = c.trailId,
        activityId = c.activityId,
        previousId = c.previousId,
        nextId = c.nextId,
        data = c.data,
        connectedBy = c.connectedBy,
        createdAt = c.createdAt)
    }


}
