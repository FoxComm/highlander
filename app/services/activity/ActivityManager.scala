package services.activity

import services._

import java.time.Instant

import scala.concurrent.{ExecutionContext, Future}

import cats.implicits._
import slick.driver.PostgresDriver.api._
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._
import utils.Slick._

import models.activity.ActivityContext
import models.activity.Activities
import models.activity.Activity

import responses.ActivityResponse

object ActivityManager {

    def activityNotFound(activityId: Int): NotFoundFailure404 = NotFoundFailure404(Activity, activityId)

    def findById(activityId: Int)(implicit ec: ExecutionContext, db: Database) : Result[ActivityResponse.Root] =
      (for { 
        activity ← * <~ Activities.mustFindById(activityId)
      } yield (ActivityResponse.build(activity))).runT(false)
}
