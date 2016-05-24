package payloads

import models.Aliases.Json

object ActivityTrailPayloads {

  case class CreateTrail(dimensionId: Int, objectId: String, data: Option[Json])

  case class AppendActivity(activityId: Int, data: Option[Json] = None)
}
