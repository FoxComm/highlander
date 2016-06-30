package services.activity

import failures.NotFoundFailure404
import models.activity.{Activities, Activity}
import responses.ActivityResponse
import services.Result
import utils.aliases._
import utils.db._

object ActivityManager {

  def activityNotFound(activityId: Int): NotFoundFailure404 =
    NotFoundFailure404(Activity, activityId)

  def findById(activityId: Int)(implicit ec: EC, db: DB): Result[ActivityResponse.Root] =
    (for {
      activity ← * <~ Activities.mustFindById404(activityId)
    } yield ActivityResponse.build(activity)).run()
}
