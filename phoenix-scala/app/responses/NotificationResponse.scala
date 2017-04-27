package responses

import io.circe.syntax._
import java.time.Instant
import models.Notification
import models.activity.ActivityContext
import payloads.NotificationActivity
import utils.aliases._
import utils.json.codecs._
import utils.json.yolo._

object NotificationResponse {
  case class Root(id: Int, kind: String, data: Json, context: ActivityContext, createdAt: Instant)
      extends ResponseItem {
    def json: Json = this.asJson
  }

  def build(n: Notification): Root = {
    val a = n.activity.extract[NotificationActivity]

    Root(id = n.id, kind = a.kind, data = a.data, context = a.context, createdAt = n.createdAt)
  }
}
