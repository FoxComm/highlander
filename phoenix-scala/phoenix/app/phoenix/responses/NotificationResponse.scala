package phoenix.responses

import java.time.Instant

import phoenix.models.Notification
import phoenix.models.activity.ActivityContext
import phoenix.payloads.NotificationActivity
import phoenix.utils.JsonFormatters
import phoenix.utils.aliases._

case class NotificationResponse(id: Int,
                                kind: String,
                                data: Json,
                                context: ActivityContext,
                                createdAt: Instant)
    extends ResponseItem

object NotificationResponse {
  implicit val formats = JsonFormatters.phoenixFormats

  def build(n: Notification): NotificationResponse = {
    val a = n.activity.extract[NotificationActivity]

    NotificationResponse(id = n.id,
                         kind = a.kind,
                         data = a.data,
                         context = a.context,
                         createdAt = n.createdAt)
  }
}
