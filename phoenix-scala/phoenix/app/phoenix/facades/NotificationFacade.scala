package phoenix.facades

import java.util.concurrent.atomic.AtomicInteger

import java.util.concurrent.{ConcurrentHashMap, ConcurrentMap}
import scala.concurrent.duration.DurationInt
import akka.NotUsed
import akka.actor._
import akka.stream._
import akka.stream.scaladsl._
import akka.stream.stage._

import com.typesafe.scalalogging.LazyLogging
import de.heikoseeberger.akkasse.scaladsl.model.{ServerSentEvent ⇒ SSE}
import org.json4s.Formats
import org.json4s.jackson.Serialization.write
import phoenix.models.account.Scope
import phoenix.models.{LastSeenNotifications, Notification, Notifications}
import phoenix.responses.NotificationResponse
import phoenix.utils.aliases._
import phoenix.utils.{JsonFormatters, NotificationListener}
import slick.jdbc.PostgresProfile.api._

/**
  * TODO: Create a new notificatons table that store notifications for an admin.
  * Don't use old activities and trails tables.
  *
  */
/*

S{x} - Session, represent each SSE connections
Db{x} - Db Listener, only one per each admin

+-----+  +-----+  +-----+
| S1  |  | S2  |  | S3  |
+-----+  +-----+  +-----+
    |    /          |
    |   /           |
   +-----+     +-----+
   | Db1 |     | Db2 |
   | A1  |     | A2  |
   +-----+     +-----+

 */

/**
  * Track downstreams and run callback when all connected downstreams will be finished.
  */
class ListenerProxy(callback: ⇒ Unit) extends GraphStage[FlowShape[SSE, SSE]] {

  val in  = Inlet[SSE]("ListenerProxy.in")
  val out = Outlet[SSE]("ListenerProxy.out")

  override val shape             = FlowShape.of(in, out)
  val downstreams: AtomicInteger = new AtomicInteger(0)

  override def createLogic(attr: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) with InHandler with OutHandler {

      override def preStart(): Unit = downstreams.incrementAndGet()

      override def onPush(): Unit = push(out, grab(in))
      override def onPull(): Unit = pull(in)

      override def onDownstreamFinish(): Unit = {
        if (downstreams.decrementAndGet() == 0) {
          callback
        }
        completeStage()
      }

      setHandlers(in, out, this)
    }
}

object NotificationFacade extends LazyLogging {
  import ConcurentHashMapExtensions._

  implicit lazy val jsonFormats: Formats = JsonFormatters.DefaultFormats

  lazy val dbListeners: ConcurrentHashMap[Int, Source[SSE, NotUsed]] = new ConcurrentHashMap()

  def streamForCurrentAdmin()(implicit au: AU,
                              ec: EC,
                              db: DB,
                              mat: Mat,
                              system: ActorSystem): Source[SSE, NotUsed] = {
    val adminId = au.account.id
    oldNotifications(adminId)
      .concat(newNotifications(adminId))
      .keepAlive(30.seconds, () ⇒ SSE.heartbeat)
  }

  private def createBroadcast[T](source: Source[T, akka.NotUsed])(implicit mat: Mat,
                                                                  ec: EC): Source[T, NotUsed] = {
    val runnableGraph: RunnableGraph[Source[T, akka.NotUsed]] =
      source.toMat(BroadcastHub.sink(bufferSize = 256))(Keep.right)

    runnableGraph.run()
  }

  private def newNotifications(
      adminId: Int)(implicit ec: EC, db: DB, mat: Mat, system: ActorSystem): Source[SSE, NotUsed] =
    dbListeners.getOrElseUpdate(
      adminId,
      _ ⇒ {
        val killSwitch = KillSwitches.shared(s"kill-db-session-$adminId")

        val (actorRef, publisher) = Source
          .actorRef[SSE](8, OverflowStrategy.fail)
          .toMat(Sink.asPublisher(false))(Keep.both)
          .run()
        system.actorOf(NotificationListener.props(adminId, actorRef), s"db-notify-listener-$adminId")

        val dbSource = Source.fromPublisher(publisher).via(killSwitch.flow)
        createBroadcast(dbSource).via(new ListenerProxy({
          killSwitch.shutdown()
          dbListeners.remove(adminId)
        }))
      }
    )

  private def oldNotifications(adminId: Int)(implicit au: AU, db: DB): Source[SSE, NotUsed] = {
    val disableAutocommit = SimpleDBIO(_.connection.setAutoCommit(false))

    val notifications = (for {
      lastSeen     ← LastSeenNotifications.findByScopeAndAccountId(Scope.current, adminId)
      notification ← Notifications.findByScopeAndAccountId(Scope.current, adminId)
      if notification.id > lastSeen.notificationId
    } yield notification).result.withStatementParameters(fetchSize = 32)

    val publisher = db.stream[Notification](disableAutocommit >> notifications)

    Source.fromPublisher(publisher).map { notification ⇒
      SSE(write(NotificationResponse.build(notification)))
    }
  }

}

object ConcurentHashMapExtensions {

  implicit class RichCMap[K, V](orig: ConcurrentMap[K, V]) {
    import java.util.function.{Function ⇒ JFunction}

    def scalaFuncToJavaFunc[T, R](func1: Function[T, R]): JFunction[T, R] =
      new JFunction[T, R] {
        override def apply(t: T): R = func1.apply(t)
      }

    def getOrElseUpdate(k: K, func: K ⇒ V): V = orig.computeIfAbsent(k, scalaFuncToJavaFunc(func))
  }
}
