package payloads

import models.activity.Aliases.Json

final case class CreateNotification(sourceDimension: String, sourceObjectId: String, activityId: Int, data: Option[Json])
