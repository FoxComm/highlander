package facades

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Keep, Sink, Source}

import de.heikoseeberger.akkasse.{EventStreamElement, ServerSentEvent ⇒ SSE}
import models.activity.{Activities, Activity, Connections, Trail, Trails}
import models.{NotificationTrailMetadata}
import models.account.Users
import org.json4s.jackson.Serialization.write
import responses.ActivityResponse
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._
import utils.{JsonFormatters, NotificationListener}

object NotificationFacade {
  implicit val formats = JsonFormatters.phoenixFormats

  def streamByAdminId(
      adminId: Int)(implicit ec: EC, db: DB, mat: Mat): Future[Source[EventStreamElement, Any]] = {
    Users.findOneByAccountId(adminId).run().map {
      case Some(admin) ⇒
        oldNotifications(adminId)
          .merge(newNotifications(adminId))
          .keepAlive(30.seconds, () ⇒ SSE.Heartbeat)
      case None ⇒
        Source.single(SSE(s"Error! User with account id=$adminId not found"))
    }
  }

  private def newNotifications(adminId: Int)(implicit ec: EC, mat: Mat): Source[SSE, Any] = {
    val (actorRef, publisher) = Source
      .actorRef[SSE](8, OverflowStrategy.fail)
      .toMat(Sink.asPublisher(false))(Keep.both)
      .run()
    new NotificationListener(adminId, msg ⇒ actorRef ! SSE(msg))
    Source.fromPublisher(publisher)
  }

  private def oldNotifications(adminId: Int)(implicit db: DB): Source[SSE, Any] = {
    val disableAutocommit = SimpleDBIO(_.connection.setAutoCommit(false))

    val activities = (for {
      trail      ← Trails.findNotificationByAdminId(adminId)
      connection ← Connections.filter(_.trailId === trail.id)
      activity   ← Activities.filter(_.id === connection.activityId)
    } yield (trail, activity)).result.withStatementParameters(fetchSize = 32)

    val publisher = db.stream[(Trail, Activity)](disableAutocommit >> activities)

    Source
      .fromPublisher(publisher)
      .filter {
        case (trail, activity) ⇒
          val lastSeen = for {
            json     ← trail.data
            metadata ← json.extractOpt[NotificationTrailMetadata]
          } yield metadata.lastSeenActivityId
          activity.id > lastSeen.getOrElse(0)
      }
      .map { case (trail, activity) ⇒ SSE(write(ActivityResponse.build(activity))) }
  }

}
