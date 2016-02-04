package payloads

import models.activity.Aliases.Json

final case class CreateTrail(dimensionId: Int, objectId: String, data: Option[Json])
final case class AppendActivity(activityId: Int, data: Option[Json] = None)
