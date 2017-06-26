package phoenix.responses

import java.time.Instant

import phoenix.models.Notification
import phoenix.models.activity.ActivityContext
import phoenix.payloads.NotificationActivity
import phoenix.utils.JsonFormatters
import phoenix.utils.aliases._

object NotificationResponse {
  implicit val formats = JsonFormatters.phoenixFormats

  case class Root(id: Int, kind: String, data: Json, context: ActivityContext, createdAt: Instant)
      extends ResponseItem

  def build(n: Notification): Root = {
    val a = n.activity.extract[NotificationActivity]

    Root(id = n.id, kind = a.kind, data = a.data, context = a.context, createdAt = n.createdAt)
  }
}
