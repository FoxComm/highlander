package phoenix.responses

import java.time.Instant

import phoenix.models.activity.{Activity, ActivityContext}
import phoenix.utils.aliases._

case class ActivityResponse(id: String,
                            kind: ActivityType,
                            data: Json,
                            context: ActivityContext,
                            createdAt: Instant)
    extends ResponseItem

object ActivityResponse {

  def build(a: Activity): ActivityResponse =
    ActivityResponse(id = a.id,
                     kind = a.activityType,
                     data = a.data,
                     context = a.context,
                     createdAt = a.createdAt)
}
