package payloads

import models.Aliases
import Aliases.Json

final case class CreateTrail(dimensionId: Int, objectId: String, data: Option[Json])
final case class AppendActivity(activityId: Int, data: Option[Json] = None)
