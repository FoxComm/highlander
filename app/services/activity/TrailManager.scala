package services.activity

import models.Aliases
import Aliases.Json
import models.activity._
import payloads.{AppendActivity, CreateTrail}
import responses.{ActivityConnectionResponse, FullActivityConnectionResponse}
import services.Result
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._
import utils.aliases._

object TrailManager {

    val autoDescription = "Automatically Generated"

    def createTrail(payload: CreateTrail)(implicit ec: EC, db: DB): Result[Int] =
      (for {
        trail ← * <~ Trails.create(
          Trail(
          dimensionId = payload.dimensionId,
          objectId = payload.objectId,
          data = payload.data))
      } yield trail.id).runTxn()


    /**
     * The append function will create a dimension and trail if they don't exist.
     * The idea here is to lazily create resources to save space and query time.
     */
    def appendActivityByObjectId(dimensionName: String, objectId: String, payload: AppendActivity)
      (implicit context: ActivityContext, ec: EC, db: DB): Result[ActivityConnectionResponse.Root] =
      appendActivityByObjectIdInner(dimensionName, objectId, payload).runTxn()

    private [services] def appendActivityByObjectIdInner(dimensionName: String, objectId: String, payload: AppendActivity,
      newTrailData: Option[Json] = None)(implicit context: ActivityContext, ec: EC, db: DB):
      DbResultT[ActivityConnectionResponse.Root] = for {

        //find or create the dimension
        dimension ← * <~ Dimensions.findOrCreateByName(dimensionName)

        //find or create
        trail ← * <~ Trails.findByObjectId(dimension.id, objectId).one.findOrCreate {
          Trails.create(Trail(dimensionId = dimension.id, objectId = objectId, data = newTrailData))
        }

        //save old tail connection id
        maybeOldTailId ← * <~ trail.tailConnectionId

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
      } yield ActivityConnectionResponse.build(objectId, dimension, newTail)

    private def updateTail(maybeOldTailId: Option[Int], newTailId: Int)(implicit ec: EC, db: DB) : DbResultT[Unit] = {
      maybeOldTailId match {
        case Some(oldTailId) ⇒
          for {
            oldTail ← * <~ Connections.mustFindById404(oldTailId)
              _ ← * <~ Connections.update(oldTail, oldTail.copy(nextId = Some(newTailId)))
          } yield Unit
        case None ⇒  pure(Unit)
      }
    }

    def findConnection(connectionId: Int)(implicit ec: EC, db: DB) : Result[FullActivityConnectionResponse.Root] = {
      (for {
        connection ← * <~ Connections.mustFindById404(connectionId)
        trail ← * <~ Trails.mustFindById404(connection.trailId)
        dimension ← * <~ Dimensions.mustFindById404(trail.dimensionId)
        activity ← * <~ Activities.mustFindById404(connection.activityId)
      } yield (FullActivityConnectionResponse.build(trail.objectId, dimension, connection, activity))).runTxn()
    }

}
