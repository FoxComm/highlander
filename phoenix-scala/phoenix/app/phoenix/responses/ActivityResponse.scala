package phoenix.responses

import java.time.Instant

import phoenix.models.activity.{Activity, ActivityContext}
import phoenix.utils.aliases._

object ActivityResponse {
  case class Root(id: String,
                  kind: ActivityType,
                  data: Json,
                  context: ActivityContext,
                  createdAt: Instant)
      extends ResponseItem

  def build(a: Activity): Root =
    Root(id = a.id,
         kind = a.activityType,
         data = a.data,
         context = a.context,
         createdAt = a.createdAt)
}
