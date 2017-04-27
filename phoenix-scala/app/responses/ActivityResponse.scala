package responses

import io.circe.syntax._
import java.time.Instant
import models.activity.{Activity, ActivityContext}
import utils.aliases._
import utils.json.codecs._

object ActivityResponse {
  case class Root(id: String,
                  kind: ActivityType,
                  data: Json,
                  context: ActivityContext,
                  createdAt: Instant)
      extends ResponseItem {
    def json: Json = this.asJson
  }

  def build(a: Activity): Root =
    Root(id = a.id,
         kind = a.activityType,
         data = a.data,
         context = a.context,
         createdAt = a.createdAt)
}
