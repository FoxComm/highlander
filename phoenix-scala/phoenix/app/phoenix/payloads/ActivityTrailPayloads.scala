package phoenix.payloads

import phoenix.utils.aliases._

object ActivityTrailPayloads {

  case class CreateTrail(dimensionId: Int, objectId: String, data: Option[Json])

  case class AppendActivity(activityId: Int, data: Option[Json] = None)
}
