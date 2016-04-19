package payloads

import models.Aliases
import Aliases.Json

case class CreateTrail(dimensionId: Int, objectId: String, data: Option[Json])
case class AppendActivity(activityId: Int, data: Option[Json] = None)
