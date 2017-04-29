package facades

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import akka.actor.{ActorRef, ActorSystem, InvalidActorNameException, Props}
import akka.stream.{ActorAttributes, OverflowStrategy, Supervision}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.pattern.{ask, pipe}

import scala.concurrent.duration._

import de.heikoseeberger.akkasse.{EventStreamElement, ServerSentEvent ⇒ SSE}
import slick.driver.PostgresDriver.api._
import models.account.Scope
import models.account.Users
import models.activity.{Activities, Activity}
import models.{LastSeenNotification, LastSeenNotifications, Notification, Notifications}
import org.json4s.jackson.Serialization.write
import responses.NotificationResponse
import utils.aliases._
import utils.db._
import utils.{JsonFormatters, NotificationListener}
import scala.collection.mutable.{Map ⇒ MutableMap}
import scala.collection.concurrent.Map
import scala.util.{Failure, Success, Try}
import com.typesafe.scalalogging.LazyLogging

import akka.stream.actor.ActorPublisher

/**
  * TODO: Create a new notificatons table that store notifications for an admin.
  * Don't use old activities and trails tables.
  *
  */
object NotificationFacade extends LazyLogging {
  implicit val formats = JsonFormatters.phoenixFormats

  val retriesToFindListenerActor              = 5
  implicit val akkaTimeout: akka.util.Timeout = akka.util.Timeout(500.millisecond)

  def streamByAdminId(adminId: Int)(
      implicit au: AU,
      ec: EC,
      db: DB,
      mat: Mat,
      system: ActorSystem): Future[Source[EventStreamElement, Any]] = {
    Users.findOneByAccountId(adminId).run().flatMap {
      case Some(admin) ⇒
        newNotifications(adminId).map { newNotificator ⇒
          oldNotifications(adminId).merge(newNotificator).keepAlive(30.seconds, () ⇒ SSE.Heartbeat)
        }

      case None ⇒
        Future.successful(Source.single(SSE(s"Error! User with account id=$adminId not found")))
    }
  }

  private def subscribeToDbListenerActor(adminId: Int, retries: Int)(
      implicit ec: EC,
      system: ActorSystem): Future[ActorRef] = {
    if (retries == 0)
      throw new RuntimeException(s"Can't subscribe for events for user $adminId")

    val listenerActorName = s"notifications-for-$adminId"
    system
      .actorSelection(s"user/$listenerActorName")
      .resolveOne(100.millisecond)
      .recoverWith[ActorRef] { // FIXME: better approach to handle concurrency conflicts
        // use just mutable.Map here ? @narma
        case _ ⇒
          try {
            val ref = system.actorOf(
                akka.actor.Props(new NotificationListener(adminId, (msg, ref) ⇒ ref ! SSE(msg))),
                listenerActorName
            )
            Future.successful(ref)
          } catch {
            case _: InvalidActorNameException ⇒
              subscribeToDbListenerActor(retriesToFindListenerActor, retries - 1)
          }
      }
  }

  private def newNotifications(
      adminId: Int)(implicit ec: EC, mat: Mat, system: ActorSystem): Future[Source[SSE, Any]] = {

    val listenerFutureRef = subscribeToDbListenerActor(adminId, retriesToFindListenerActor)
    listenerFutureRef.flatMap { ref ⇒
      (ref ? NotificationListener.NewClientConnected).mapTo[ActorRef]

    }.map { childRef ⇒
      val ssePublisher = ActorPublisher[SSE](childRef)
      Source.fromPublisher(ssePublisher)
    }
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
      .map { case (lastSeen, notification) ⇒ SSE(write(NotificationResponse.build(notification))) }
  }

}
