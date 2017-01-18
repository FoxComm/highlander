package services

import de.heikoseeberger.akkasse.{ServerSentEvent ⇒ SSE}
import failures._
import models.Notification._
import models.activity._
import models.account._
import models.{
  NotificationTrailMetadata,
  NotificationSubscription ⇒ Sub,
  NotificationSubscriptions ⇒ Subs
}
import org.json4s.Extraction.decompose
import org.json4s.jackson.Serialization.write
import org.postgresql.core.{Utils ⇒ PgjdbcUtils}
import payloads.ActivityTrailPayloads.AppendActivity
import payloads.CreateNotification
import responses.{
  ActivityConnectionResponse,
  ActivityResponse,
  LastSeenActivityResponse,
  TheResponse
}
import services.activity.TrailManager
import slick.driver.PostgresDriver.api._
import utils.JsonFormatters
import utils.aliases._
import utils.db._

object NotificationManager {
  implicit val formats = JsonFormatters.phoenixFormats

  def createNotification(payload: CreateNotification)(
      implicit ac: AC,
      ec: EC,
      db: DB): DbResultT[Seq[ActivityConnectionResponse.Root]] =
    for {
      sourceDimensionId ← * <~ dimensionIdByName(payload.sourceDimension)
      activity          ← * <~ Activities.mustFindById400(payload.activityId)
      adminIds ← * <~ Subs
        .findByDimensionAndObject(sourceDimensionId, payload.sourceObjectId)
        .map(_.adminId)
        .result
      response ← * <~ adminIds.map { adminId ⇒
        val appendActivity = AppendActivity(payload.activityId, payload.data)
        val newTrailData   = Some(decompose(NotificationTrailMetadata(0)))
        TrailManager.appendActivityByObjectIdInner(Dimension.notification,
                                                   adminId.toString,
                                                   appendActivity,
                                                   newTrailData)
      }
      _ ← * <~ DBIO.sequence(adminIds.map { adminId ⇒
        val payload        = write(ActivityResponse.build(activity))
        val escapedPayload = PgjdbcUtils.escapeLiteral(null, payload, false).toString
        sqlu"NOTIFY #${notificationChannel(adminId)}, '#$escapedPayload'"
      })
    } yield response

  def updateLastSeen(adminId: Int, activityId: Int)(implicit ec: EC,
                                                    db: DB): DbResultT[LastSeenActivityResponse] =
    for {
      _ ← * <~ Accounts.mustFindById404(adminId)
      _ ← * <~ Activities.mustFindById404(activityId)
      trail ← * <~ Trails
        .findNotificationByAdminId(adminId)
        .mustFindOneOr(NotificationTrailNotFound400(adminId))
      _ ← * <~ Trails
        .update(trail, trail.copy(data = Some(decompose(NotificationTrailMetadata(activityId)))))
    } yield LastSeenActivityResponse(trailId = trail.id, lastSeenActivityId = activityId)

  def subscribe(adminIds: Seq[Int], objectIds: Seq[String], reason: Sub.Reason, dimension: String)(
      implicit ec: EC): DbResultT[TheResponse[Option[Int]]] =
    for {
      dimension  ← * <~ Dimensions.findOrCreateByName(dimension)
      realAdmins ← * <~ Users.filter(_.accountId.inSet(adminIds)).map(_.accountId).result
      requestedSubs = for (adminId ← realAdmins; objectId ← objectIds) yield (adminId, objectId)
      partialFilter = Subs.filter(_.dimensionId === dimension.id).filter(_.reason === reason)
      existingSubs ← * <~ DBIO
        .sequence(requestedSubs.map {
          case (adminId, objectId) ⇒
            partialFilter
              .filter(_.adminId === adminId)
              .filter(_.objectId === objectId)
              .map(sub ⇒ (sub.adminId, sub.objectId))
              .result
        })
        .map(_.flatten)
      newSubsQty ← * <~ Subs.createAll(requestedSubs.diff(existingSubs).map {
        case (adminId, objectId) ⇒
          Sub(adminId = adminId, objectId = objectId, dimensionId = dimension.id, reason = reason)
      })
      warnings = Failures(adminIds.diff(realAdmins).map(NotFoundFailure404(User, _)): _*)
    } yield TheResponse.build(value = newSubsQty, warnings = warnings)

  def unsubscribe(adminIds: Seq[Int],
                  objectIds: Seq[String],
                  reason: Sub.Reason,
                  dimension: String)(implicit ec: EC): DbResultT[Unit] =
    for {
      d ← * <~ Dimensions.findByName(dimension).one
      _ ← * <~ d.fold(DbResultT.unit) { dimension ⇒
        Subs
          .filter(_.dimensionId === dimension.id)
          .filter(_.adminId.inSet(adminIds))
          .filter(_.objectId.inSet(objectIds))
          .filter(_.reason === reason)
          .deleteAll(onSuccess = DbResultT.unit, onFailure = DbResultT.unit)
      }
    } yield {}

  private def dimensionIdByName(name: String)(implicit ec: EC) =
    Dimensions.findByName(name).map(_.id).mustFindOneOr(NotFoundFailure400(Dimension, name))
}
