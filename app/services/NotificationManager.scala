package services

import scala.concurrent.duration.DurationInt
import akka.actor.{ActorSystem, Props}
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.Source

import de.heikoseeberger.akkasse.{ServerSentEvent, WithHeartbeats}
import models.Notification._
import models.activity._
import models.{NotificationSubscription ⇒ Sub, NotificationSubscriptions ⇒ Subs, NotificationTrailMetadata, StoreAdmin, StoreAdmins}
import org.json4s.Extraction.decompose
import org.json4s.jackson.Serialization.write
import payloads.{AppendActivity, CreateNotification}
import responses.{ActivityConnectionResponse, LastSeenActivityResponse, TheResponse}
import services.activity.TrailManager
import services.actors.NotificationPublisher
import slick.driver.PostgresDriver.api._
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.DbResult
import utils.Slick.implicits._
import utils.{DbResultT, JsonFormatters}
import utils.aliases._

object NotificationManager {
  implicit val formats = JsonFormatters.phoenixFormats

  def streamByAdminId(id: StoreAdmin#Id)(implicit ec: EC, db: DB, system: ActorSystem): Source[ServerSentEvent, Unit] = {
    val dataPublisherRef = system.actorOf(Props(new NotificationPublisher(id)))
    val dataPublisher = ActorPublisher[String](dataPublisherRef)
    dataPublisherRef ! id

    Source.fromPublisher(dataPublisher)
      .map(ServerSentEvent(_))
      .via(WithHeartbeats(30.seconds))
  }

  def createNotification(payload: CreateNotification)(implicit ac: ActivityContext, ec: EC, db: DB):
  Result[Seq[ActivityConnectionResponse.Root]] = (for {
    sourceDimensionId ← * <~ dimensionIdByName(payload.sourceDimension)
    activity ← * <~ Activities.mustFindById400(payload.activityId)
    adminIds ← * <~ Subs.findByDimensionAndObject(sourceDimensionId, payload.sourceObjectId).map(_.adminId).result.toXor
    response ← * <~ DbResultT.sequence(adminIds.map { adminId ⇒
      val appendActivity = AppendActivity(payload.activityId, payload.data)
      val newTrailData = Some(decompose(NotificationTrailMetadata(0)))
      TrailManager.appendActivityByObjectIdInner(Dimension.notification, adminId.toString, appendActivity, newTrailData)
    })
    _ ← * <~ DBIO.sequence(adminIds.map { adminId ⇒
      sqlu"NOTIFY #${notificationChannel(adminId)}, '#${write(activity)}'"
    }).toXor
  } yield response).runTxn()

  def updateLastSeen(adminId: Int, activityId: Int)(implicit ec: EC, db: DB): Result[LastSeenActivityResponse] = (for {
    _ ← * <~ StoreAdmins.mustFindById404(adminId)
    _ ← * <~ Activities.mustFindById404(activityId)
    trail ← * <~ Trails.findNotificationByAdminId(adminId).one.mustFindOr(NotificationTrailNotFound400(adminId))
    _ ← * <~ Trails.update(trail, trail.copy(data = Some(decompose(NotificationTrailMetadata(activityId)))))
  } yield LastSeenActivityResponse(trailId = trail.id, lastSeenActivityId = activityId)).runTxn()

  def subscribe(adminIds: Seq[Int], objectIds: Seq[String], reason: Sub.Reason, dimension: String)
    (implicit ec: EC, db: DB): DbResultT[TheResponse[Option[Int]]] = for {
    dimension     ← * <~ Dimensions.findOrCreateByName(dimension)
    realAdmins    ← * <~ StoreAdmins.filter(_.id.inSet(adminIds)).map(_.id).result.toXor
    requestedSubs = for (adminId ← realAdmins; objectId ← objectIds) yield (adminId, objectId)
    partialFilter = Subs.filter(_.dimensionId === dimension.id).filter(_.reason === reason)
    existingSubs  ← * <~ DBIO.sequence(requestedSubs.map { case (adminId, objectId) ⇒
                           partialFilter
                             .filter(_.adminId === adminId)
                             .filter(_.objectId === objectId)
                             .map(sub ⇒ (sub.adminId, sub.objectId)).result
                         }).map(_.flatten).toXor
    newSubsQty    ← * <~ Subs.createAll(requestedSubs.diff(existingSubs).map { case (adminId, objectId) ⇒
                           Sub(adminId = adminId, objectId = objectId, dimensionId = dimension.id, reason = reason)
                         })
    warnings      = Failures(adminIds.diff(realAdmins).map(NotFoundFailure404(StoreAdmin, _)): _*)
  } yield TheResponse.build(value = newSubsQty, warnings = warnings)

  def unsubscribe(adminIds: Seq[Int], objectIds: Seq[String], reason: Sub.Reason, dimension: String)
    (implicit ec: EC, db: DB): DbResultT[Unit] = for {
    d ← * <~ Dimensions.findByName(dimension).one.toXor
    _ ← * <~ d.fold(DbResult.unit) { dimension ⇒
      Subs
        .filter(_.dimensionId === dimension.id)
        .filter(_.adminId.inSet(adminIds))
        .filter(_.objectId.inSet(objectIds))
        .filter(_.reason === reason)
        .deleteAll(onSuccess = DbResult.unit, onFailure = DbResult.unit)
    }
  } yield {}

  private def dimensionIdByName(name: String)(implicit ec: EC) =
    Dimensions.findByName(name).map(_.id).one.mustFindOr(NotFoundFailure400(Dimension, name))
}
