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
import models.activity.Trails
import models.activity.Trail
import models.activity.Connections
import models.activity.Connection

import payloads.CreateTrail
import payloads.AppendActivity

object TrailManager {

    def trailNotFound(dimensionId: Int, objectId: Int): NotFoundFailure404 = NotFoundFailure404(Trail, (dimensionId, objectId))
    def connectionNotFound(connectionId: Int): NotFoundFailure404 = NotFoundFailure404(Connection, connectionId)

    def createTrail(payload: Trail)
    (implicit ec: ExecutionContext, db: Database): Result[Int] = 
      (for { 
        trail ← * <~ Trails.create(
          Trail(
          dimensionId = payload.dimensionId, 
          objectId = payload.objectId,
          data = payload.data))
      } yield trail.id).runT()
    

    def appendActivityByObjectId(
      dimensionId: Int, 
      objectId: Int, 
      payload: AppendActivity) 
    (implicit context: ActivityContext, ec: ExecutionContext, db: Database) : Result[Int] =
      (for { 

        //find or create
        trail ← * <~ Trails.findByObjectId(dimensionId, objectId).one.findOrCreate { 
          Trails.create(Trail(dimensionId = dimensionId, objectId = objectId))
        }

        //save old tail connection id
        maybeOldTailId ←  * <~ trail.tailConnectionId

        //insert new tail, point previous to old tail
        newTail ← * <~ Connections.create(Connection(
          dimensionId = dimensionId, 
          trailId = trail.id, 
          activityId = payload.activityId,
          previousId = maybeOldTailId,
          nextId = None,
          data = payload.data,
          connectedBy = context))

        //update trail to point to new tail
        updatedTrail ← * <~ Trails.update(trail, trail.copy(tailConnectionId = Some(newTail.id)))

        //update old tail if there was one
        _ ← * <~ updateTail(maybeOldTailId, newTail.id)
      } yield (newTail.id)).runT()

    private def updateTail(maybeOldTailId: Option[Int], newTailId: Int) 
    (implicit ec: ExecutionContext, db: Database) : DbResultT[Unit] = { 
      maybeOldTailId match {
        case Some(oldTailId) ⇒  
          for {
            oldTail ← * <~ Connections.findById(oldTailId).extract.one.mustFindOr(connectionNotFound(oldTailId))
              _ ← * <~ Connections.update(oldTail, oldTail.copy(nextId = Some(newTailId)))
          } yield Unit
        case None ⇒  pure(Unit)
      }
    }

}
