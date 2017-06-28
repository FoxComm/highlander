package phoenix.services

import cats.implicits._
import core.db._
import core.failures._
import de.heikoseeberger.akkasse.scaladsl.model.{ServerSentEvent ⇒ SSE}
import org.json4s.Extraction.decompose
import org.json4s.jackson.JsonMethods._
import org.postgresql.core.{Utils ⇒ PgjdbcUtils}
import phoenix.models.Notification._
import phoenix.models.account._
import phoenix.models.activity._
import phoenix.models.{LastSeenNotification, LastSeenNotifications, Notification, Notifications, NotificationSubscription ⇒ Sub, NotificationSubscriptions ⇒ Subs}
import phoenix.payloads.CreateNotification
import phoenix.responses.{ActivityResponse, LastSeenNotificationResponse, NotificationResponse, TheResponse}
import phoenix.utils.JsonFormatters
import phoenix.utils.aliases._
import slick.jdbc.PostgresProfile.api._

object NotificationManager {
  implicit val formats = JsonFormatters.phoenixFormats

  def createNotification(payload: CreateNotification)(implicit ac: AC,
                                                      au: AU,
                                                      ec: EC,
                                                      db: DB): DbResultT[ActivityResponse.Root] =
    for {
      dimension ← * <~ Dimensions.findOrCreateByName(payload.sourceDimension)
      activity = Activity(
        id = payload.activity.id,
        activityType = payload.activity.kind,
        data = payload.activity.data,
        context = payload.activity.context,
        createdAt = payload.activity.createdAt
      )

      adminIds ← * <~ Subs
                  .findByDimensionAndObject(dimension.id, payload.sourceObjectId)
                  .map(_.adminId)
                  .result
      response ← * <~ ActivityResponse.build(activity)

      notifications = adminIds.toList.map { adminId ⇒
        Notification(scope = payload.activity.context.scope,
                     accountId = adminId,
                     dimensionId = dimension.id,
                     objectId = payload.sourceObjectId,
                     activity = decompose(payload.activity))
      }

      createdNotifications ← * <~ Notifications.createAllReturningModels(notifications)

      _ ← * <~ DBIO.sequence(createdNotifications.map { notification ⇒
           var payload        = compact(decompose(NotificationResponse.build(notification)))
           var escapedPayload = PgjdbcUtils.escapeLiteral(null, payload, false).toString
           sqlu"NOTIFY #${notificationChannel(notification.accountId)}, '#$escapedPayload'"
         })
    } yield response

  def updateLastSeen(accountId: Int, notificationId: Int)(implicit au: AU,
                                                          ec: EC,
                                                          db: DB): DbResultT[LastSeenNotificationResponse] =
    for {
      _ ← * <~ Accounts.mustFindById404(accountId)
      scope = Scope.current
      lastSeen ← * <~ LastSeenNotifications
                  .findByScopeAndAccountId(scope, accountId)
                  .one
                  .findOrCreate(
                    LastSeenNotifications.create(
                      LastSeenNotification(scope = scope,
                                           accountId = accountId,
                                           notificationId = notificationId)))
      _ ← * <~ LastSeenNotifications.update(lastSeen, lastSeen.copy(notificationId = notificationId))
    } yield LastSeenNotificationResponse(lastSeenNotificationId = notificationId)

  def subscribe(adminIds: Seq[Int], objectIds: Seq[String], reason: Sub.Reason, dimension: String)(
      implicit ec: EC): DbResultT[TheResponse[Option[Int]]] =
    for {
      dimension  ← * <~ Dimensions.findOrCreateByName(dimension)
      realAdmins ← * <~ Users.filter(_.accountId.inSet(adminIds)).map(_.accountId).result
      requestedSubs = for {
        adminId  ← realAdmins
        objectId ← objectIds
      } yield (adminId, objectId)
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

  def unsubscribe(adminIds: Seq[Int], objectIds: Seq[String], reason: Sub.Reason, dimension: String)(
      implicit ec: EC): DbResultT[Unit] =
    for {
      d ← * <~ Dimensions.findByName(dimension).one
      _ ← * <~ d.fold(().pure[DbResultT]) { dimension ⇒
           Subs
             .filter(_.dimensionId === dimension.id)
             .filter(_.adminId.inSet(adminIds))
             .filter(_.objectId.inSet(objectIds))
             .filter(_.reason === reason)
             .deleteAll
             .void
         }
    } yield ()

  private def dimensionIdByName(name: String)(implicit ec: EC) =
    Dimensions.findByName(name).map(_.id).mustFindOneOr(NotFoundFailure400(Dimension, name))
}
