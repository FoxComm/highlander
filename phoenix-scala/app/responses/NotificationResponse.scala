package responses

import java.time.Instant
import models.Notification
import models.activity.ActivityContext
import payloads.NotificationActivity
import utils.aliases._

object NotificationResponse {
  case class Root(id: Int, kind: String, data: Json, context: ActivityContext, createdAt: Instant)
      extends ResponseItem

  def build(n: Notification): Root = {
    val a = n.activity.extract[NotificationActivity]

    Root(id = n.id, kind = a.kind, data = a.data, context = a.context, createdAt = n.createdAt)
  }
}
