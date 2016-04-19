package responses

import java.time.Instant

import models.Aliases
import models.activity.{Activity, ActivityContext}
import Aliases.{ActivityType, Json}

object ActivityResponse {
  case class Root(
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
