package services

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import akka.stream.scaladsl._
import akka.stream.{Materializer, OverflowStrategy}

import de.heikoseeberger.akkasse.{ServerSentEvent ⇒ SSE}
import failures._
import models.Notification._
import models.activity._
import models.{NotificationTrailMetadata, StoreAdmin, StoreAdmins, NotificationSubscription ⇒ Sub, NotificationSubscriptions ⇒ Subs}
import org.json4s.Extraction.decompose
import org.json4s.jackson.Serialization.write
import org.postgresql.core.{Utils ⇒ PgjdbcUtils}
import payloads.{AppendActivity, CreateNotification}
import responses.{ActivityConnectionResponse, ActivityResponse, LastSeenActivityResponse, TheResponse}
import services.activity.TrailManager
import slick.driver.PostgresDriver.api._
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.DbResult
import utils.Slick.implicits._
import utils.aliases._
import utils.{DbResultT, JsonFormatters, NotificationListener}

object NotificationManager {
  implicit val formats = JsonFormatters.phoenixFormats

  def streamByAdminId(adminId: StoreAdmin#Id)
    (implicit ec: EC, db: DB, mat: Materializer): Future[Source[SSE, Any]] = {
    StoreAdmins.findOneById(adminId).run().map {
      case Some(admin) ⇒
        oldNotifications(adminId).merge(newNotifications(adminId))
          .keepAlive(30.seconds, () ⇒ SSE.heartbeat)
      case None ⇒
        Source.single(SSE(s"Error! Store admin with id=$adminId not found"))
    }
  }

  private def newNotifications(adminId: Int)(implicit ec: EC, mat: Materializer): Source[SSE, Any] = {
    val (actorRef, publisher) = Source.actorRef[SSE](8, OverflowStrategy.fail)
      .toMat(Sink.asPublisher(false))(Keep.both).run()
    new NotificationListener(adminId, msg ⇒ actorRef ! SSE(msg))
    Source.fromPublisher(publisher)
  }

  private def oldNotifications(adminId: Int)(implicit db: DB): Source[SSE, Any] = {
    val disableAutocommit = SimpleDBIO(_.connection.setAutoCommit(false))

    val activities = (for {
      trail ← Trails.findNotificationByAdminId(adminId)
      connection ← Connections.filter(_.trailId === trail.id)
      activity ← Activities.filter(_.id === connection.activityId)
    } yield (trail, activity)).result.withStatementParameters(fetchSize = 32)

    val publisher = db.stream[(Trail, Activity)](disableAutocommit >> activities)

    Source.fromPublisher(publisher)
      .filter { case (trail, activity) ⇒
        val lastSeen = for {
          json ← trail.data
          metadata ← json.extractOpt[NotificationTrailMetadata]
        } yield metadata.lastSeenActivityId
        activity.id > lastSeen.getOrElse(0)
      }
      .map { case (trail, activity) ⇒ SSE(write(ActivityResponse.build(activity))) }
  }

  @SuppressWarnings(Array("org.brianmckenna.wartremover.warts.Null"))
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
      val payload = write(ActivityResponse.build(activity))
      val escapedPayload = PgjdbcUtils.escapeLiteral(null, payload, false).toString
      sqlu"NOTIFY #${notificationChannel(adminId)}, '#${escapedPayload}'"
    }).toXor
  } yield response).runTxn()

  def updateLastSeen(adminId: Int, activityId: Int)(implicit ec: EC, db: DB): Result[LastSeenActivityResponse] = (for {
    _ ← * <~ StoreAdmins.mustFindById404(adminId)
    _ ← * <~ Activities.mustFindById404(activityId)
    trail ← * <~ Trails.findNotificationByAdminId(adminId).one.mustFindOr(NotificationTrailNotFound400(adminId))
    _ ← * <~ Trails.update(trail, trail.copy(data = Some(decompose(NotificationTrailMetadata(activityId)))))
  } yield LastSeenActivityResponse(trailId = trail.id, lastSeenActivityId = activityId)).runTxn()

  def subscribe(adminIds: Seq[Int], objectIds: Seq[String], reason: Sub.Reason, dimension: String)
    (implicit ec: EC): DbResultT[TheResponse[Option[Int]]] = for {
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
    (implicit ec: EC): DbResultT[Unit] = for {
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
