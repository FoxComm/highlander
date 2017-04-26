package facades

import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Keep, Sink, Source}
import de.heikoseeberger.akkasse.{EventStreamElement, ServerSentEvent ⇒ SSE}
import io.circe.jackson.syntax._
import io.circe.syntax._
import models.account.{Scope, Users}
import models.{LastSeenNotification, LastSeenNotifications, Notification, Notifications}
import responses.NotificationResponse
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import slick.driver.PostgresDriver.api._
import utils.NotificationListener
import utils.aliases._
import utils.db._
import utils.json.codecs._

/**
  * TODO: Create a new notificatons table that store notifications for an admin.
  * Don't use old activities and trails tables.
  *
  */
object NotificationFacade {
  def streamByAdminId(adminId: Int)(implicit au: AU,
                                    ec: EC,
                                    db: DB,
                                    mat: Mat): Future[Source[EventStreamElement, Any]] = {
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

  private def oldNotifications(adminId: Int)(implicit au: AU, db: DB): Source[SSE, Any] = {
    val disableAutocommit = SimpleDBIO(_.connection.setAutoCommit(false))

    val notifications = (for {
      lastSeen     ← LastSeenNotifications.findByScopeAndAccountId(Scope.current, adminId)
      notification ← Notifications.findByScopeAndAccountId(Scope.current, adminId)
    } yield (lastSeen, notification)).result.withStatementParameters(fetchSize = 32)

    val publisher =
      db.stream[(LastSeenNotification, Notification)](disableAutocommit >> notifications)

    Source
      .fromPublisher(publisher)
      .filter {
        case (lastSeen, notification) ⇒
          notification.id > lastSeen.notificationId
      }
      .map {
        case (_, notification) ⇒ SSE(NotificationResponse.build(notification).asJson.jacksonPrint)
      }
  }

}
