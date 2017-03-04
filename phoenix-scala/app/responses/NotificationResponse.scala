package responses

import java.time.Instant

import models.Notification
import models.activity.{Activity, ActivityContext}
import utils.JsonFormatters
import utils.aliases._

object NotificationResponse {
  implicit val formats = JsonFormatters.phoenixFormats

  case class Root(id: Int,
                  kind: ActivityType,
                  data: Json,
                  context: ActivityContext,
                  createdAt: Instant)
      extends ResponseItem

  def build(n: Notification): Root = {
    val a = n.activity.extract[ActivityResponse.Root]

    Root(id = n.id, kind = a.kind, data = a.data, context = a.context, createdAt = n.createdAt)
  }
}
