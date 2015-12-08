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
import models.activity.Connection
import models.activity.Connections
import models.activity.Dimension
import models.activity.Dimensions
import models.activity.Trail
import models.activity.Trails

import payloads.CreateTrail
import payloads.AppendActivity

import responses.ActivityConnectionResponse

object TrailManager {

    val dimensionDescription = "Automatically Generated"

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
    

    /**
     * The append function will create a dimension and trail if they don't exist.
     * The idea here is to lazily create resources to save space and query time.
     */
    def appendActivityByObjectId(
      dimensionName: String, 
      objectId: Int, 
      payload: AppendActivity) 
    (implicit context: ActivityContext, ec: ExecutionContext, db: Database) : Result[ActivityConnectionResponse.Root] =
      (for { 

        //find or create the dimension
        dimension ← * <~ Dimensions.findByName(dimensionName).one.findOrCreate { 
          Dimensions.create(Dimension(name = dimensionName, description = dimensionDescription))
        }

        //find or create
        trail ← * <~ Trails.findByObjectId(dimension.id, objectId).one.findOrCreate { 
          Trails.create(Trail(dimensionId = dimension.id, objectId = objectId))
        }

        //save old tail connection id
        maybeOldTailId ←  * <~ trail.tailConnectionId

        //insert new tail, point previous to old tail
        newTail ← * <~ Connections.create(Connection(
          dimensionId = dimension.id, 
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
      } yield (ActivityConnectionResponse.build(objectId, dimension, newTail))).runT()

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
