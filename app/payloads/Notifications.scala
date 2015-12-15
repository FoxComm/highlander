package payloads

import models.activity.Aliases.Json

final case class CreateNotification(sourceDimension: String, sourceObjectId: Int, activityId: Int, data: Option[Json])
