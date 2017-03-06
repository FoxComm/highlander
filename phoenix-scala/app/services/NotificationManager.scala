package services

import de.heikoseeberger.akkasse.{ServerSentEvent ⇒ SSE}
import failures._
import models.{Notification, Notifications}
import models.{LastSeenNotification, LastSeenNotifications}
import models.Notification._
import models.activity._
import models.account._
import models.{NotificationSubscription ⇒ Sub, NotificationSubscriptions ⇒ Subs}
import org.json4s.Extraction.decompose
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization.write
import org.postgresql.core.{Utils ⇒ PgjdbcUtils}
import payloads.ActivityTrailPayloads.AppendActivity
import payloads.CreateNotification
import responses.{ActivityResponse, LastSeenNotificationResponse, NotificationResponse, TheResponse}
import slick.driver.PostgresDriver.api._
import utils.JsonFormatters
import utils.aliases._
import utils.db._

object NotificationManager {
  implicit val formats = JsonFormatters.phoenixFormats

  def createNotification(payload: CreateNotification)(implicit ac: AC,
                                                      au: AU,
                                                      ec: EC,
                                                      db: DB): DbResultT[ActivityResponse.Root] = {
    for {
      dimension ← * <~ Dimensions.findOrCreateByName(payload.sourceDimension)
      activity ← * <~ Activity(id = payload.activity.id,
                               activityType = payload.activity.kind,
                               data = payload.activity.data,
                               context = payload.activity.context,
                               createdAt = payload.activity.createdAt)

      adminIds ← * <~ Subs
                  .findByDimensionAndObject(dimension.id, payload.sourceObjectId)
                  .map(_.adminId)
                  .result
      response ← * <~ ActivityResponse.build(activity)

      notifications ← * <~ adminIds.toList.map { adminId ⇒
                       Notifications.create(
                           Notification(scope = payload.activity.context.scope,
                                        accountId = adminId,
                                        dimensionId = dimension.id,
                                        objectId = payload.sourceObjectId,
                                        activity = decompose(payload.activity)))
                     }

      _ ← * <~ DBIO.sequence(notifications.map { notification ⇒
           var payload        = compact(decompose(NotificationResponse.build(notification)))
           var escapedPayload = PgjdbcUtils.escapeLiteral(null, payload, false).toString
           sqlu"NOTIFY #${notificationChannel(notification.accountId)}, '#$escapedPayload'"
         })
    } yield response
  }

  def updateLastSeen(accountId: Int, notificationId: Int)(
      implicit au: AU,
      ec: EC,
      db: DB): DbResultT[LastSeenNotificationResponse] =
    for {
      scope ← * <~ Scope.current
      _     ← * <~ Accounts.mustFindById404(accountId)
      lastSeen ← * <~ LastSeenNotifications
                  .findByScopeAndAccountId(scope, accountId)
                  .one
                  .findOrCreate(
                      LastSeenNotifications.create(
                          LastSeenNotification(scope = scope,
                                               accountId = accountId,
                                               notificationId = notificationId)))
      _ ← * <~ LastSeenNotifications.update(lastSeen,
                                            lastSeen.copy(notificationId = notificationId))
    } yield LastSeenNotificationResponse(lastSeenNotificationId = notificationId)

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
                      Sub(adminId = adminId,
                          objectId = objectId,
                          dimensionId = dimension.id,
                          reason = reason)
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
