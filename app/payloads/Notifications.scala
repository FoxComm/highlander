package payloads

import models.Aliases
import Aliases.Json

final case class CreateNotification(sourceDimension: String, sourceObjectId: String, activityId: Int, data: Option[Json])
