package payloads

import models.activity.Aliases.Json

final case class CreateTrail(dimensionId: Int, objectId: Int, data: Json)
final case class AppendActivity(activityId: Int, data: Json)
