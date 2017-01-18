package responses

import java.time.Instant

import models.activity._
import utils.aliases._

object ActivityConnectionResponse {

  case class Root(id: Int,
                  dimension: String,
                  objectId: String,
                  trailId: Int,
                  activityId: Int,
                  data: Option[Json],
                  connectedBy: ActivityContext,
                  createdAt: Instant)
      extends ResponseItem

  def build(objectId: String, d: Dimension, c: Connection): Root = {
    Root(id = c.id,
         dimension = d.name,
         objectId = objectId,
         trailId = c.trailId,
         activityId = c.activityId,
         data = c.data,
         connectedBy = c.connectedBy,
         createdAt = c.createdAt)
  }
}

object FullActivityConnectionResponse {

  case class ActivityResp(id: Int,
                          kind: ActivityType,
                          data: Json,
                          context: ActivityContext,
                          createdAt: Instant)

  case class Root(id: Int,
                  dimension: String,
                  objectId: String,
                  trailId: Int,
                  activity: ActivityResp,
                  data: Option[Json],
                  connectedBy: ActivityContext,
                  createdAt: Instant)
      extends ResponseItem

  def build(objectId: String, d: Dimension, c: Connection, a: Activity): Root = {
    Root(id = c.id,
         dimension = d.name,
         objectId = objectId,
         trailId = c.trailId,
         activity = ActivityResp(
           id = a.id,
           kind = a.activityType,
           data = a.data,
           context = a.context,
           createdAt = a.createdAt
         ),
         data = c.data,
         connectedBy = c.connectedBy,
         createdAt = c.createdAt)
  }
}
