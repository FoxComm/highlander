package services.activity

import models.activity._
import payloads.ActivityTrailPayloads._
import responses.{ActivityConnectionResponse, FullActivityConnectionResponse}
import utils.aliases._
import utils.db._

object TrailManager {

  val autoDescription = "Automatically Generated"

  def createTrail(payload: CreateTrail)(implicit ec: EC, db: DB): DbResultT[Int] =
    for {
      trail ← * <~ Trails.create(
        Trail(dimensionId = payload.dimensionId, objectId = payload.objectId, data = payload.data))
    } yield trail.id

  /**
    * The append function will create a dimension and trail if they don't exist.
    * The idea here is to lazily create resources to save space and query time.
    */
  def appendActivityByObjectId(dimensionName: String, objectId: String, payload: AppendActivity)(
      implicit context: AC,
      ec: EC,
      db: DB): DbResultT[ActivityConnectionResponse.Root] =
    appendActivityByObjectIdInner(dimensionName, objectId, payload)

  private[services] def appendActivityByObjectIdInner(dimensionName: String,
                                                      objectId: String,
                                                      payload: AppendActivity,
                                                      newTrailData: Option[Json] = None)(
      implicit context: AC,
      ec: EC,
      db: DB): DbResultT[ActivityConnectionResponse.Root] =
    for {

      //find or create the dimension
      dimension ← * <~ Dimensions.findOrCreateByName(dimensionName)

      //find or create
      trail ← * <~ Trails.findByObjectId(dimension.id, objectId).one.findOrCreate {
        Trails.create(Trail(dimensionId = dimension.id, objectId = objectId, data = newTrailData))
      }

      //insert new tail, point previous to old tail
      newTail ← * <~ Connections.create(
        Connection(dimensionId = dimension.id,
                   trailId = trail.id,
                   activityId = payload.activityId,
                   data = payload.data,
                   connectedBy = context))

      //update trail to point to new tail
      updatedTrail ← * <~ Trails.update(trail, trail.copy(tailConnectionId = Some(newTail.id)))
    } yield ActivityConnectionResponse.build(objectId, dimension, newTail)

  def findConnection(connectionId: Int)(implicit ec: EC,
                                        db: DB): DbResultT[FullActivityConnectionResponse.Root] = {
    for {
      connection ← * <~ Connections.mustFindById404(connectionId)
      trail      ← * <~ Trails.mustFindById404(connection.trailId)
      dimension  ← * <~ Dimensions.mustFindById404(trail.dimensionId)
      activity   ← * <~ Activities.mustFindById404(connection.activityId)
    } yield FullActivityConnectionResponse.build(trail.objectId, dimension, connection, activity)

  }
}
