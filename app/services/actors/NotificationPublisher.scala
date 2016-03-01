package services.actors

import scala.util.{Failure, Success}
import akka.actor.ActorLogging
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.Cancel

import com.github.mauricio.async.db.postgresql.PostgreSQLConnection
import com.github.mauricio.async.db.postgresql.util.URLParser
import models.Notification._
import models.activity._
import models.{NotificationTrailMetadata, StoreAdmins}
import org.json4s.jackson.Serialization.write
import utils.ExPostgresDriver.api._
import utils.JsonFormatters
import utils.Slick.implicits._
import utils.aliases._

class NotificationPublisher(adminId: Int)(implicit ec: EC, db: DB)
  extends ActorPublisher[String] with ActorLogging {

  implicit val formats = JsonFormatters.phoenixFormats

  private val connection = createConnection()

  override def receive = {
    case adminId: Int ⇒
      StoreAdmins.findById(adminId).result.headOption.run().map {
        case Some(_) ⇒
          connection.connect.onComplete {
            case Success(_) ⇒ self ! Start
            case Failure(e) ⇒ log.error(s"Failed to establish connection for publisher! Error: ${e.getMessage}")
          }

        case None ⇒
          push(s"Error! Store admin with id=$adminId not found")
          onCompleteThenStop()
      }

    case Start ⇒
      if (connection.isConnected) {
        loadUnread()
        connection.sendQuery(s"LISTEN ${notificationChannel(adminId)}")
        connection.registerNotifyListener { message ⇒ push(message.payload) }
      } else {
        log.error("Invalid attempt to start publisher, connection is not active!")
      }

    case Cancel ⇒
      connection.disconnect
      context.stop(self)
  }

  override def postStop() = {
    connection.disconnect
    super.postStop()
  }

  private def createConnection() = {
    val config = utils.Config.loadWithEnv()
    val configuration = URLParser.parse(config.getString("db.url"))
    new PostgreSQLConnection(configuration)
  }

  private def loadUnread() = {
    val fetchUnreadActivities = for {
      // There must be only 1 notification trail, but working with Seq here is simpler than with Option
      trail ← Trails.findNotificationByAdminId(adminId).result
      connections ← Connections.filter(_.trailId.inSet(trail.map(_.id))).result
      activities ← Activities.filter(_.id.inSet(connections.map(_.activityId))).result
      lastSeenId = trail.headOption.flatMap(_.data.map {
        _.extract[NotificationTrailMetadata].lastSeenActivityId
      }).getOrElse(-1)
    } yield activities.filter(_.id > lastSeenId)

    fetchUnreadActivities.run().foreach(_.foreach(json ⇒ push(write(json))))
  }

  private def push(message: String) =
    if (isActive && totalDemand > 0) this.onNext(message)
    else onCompleteThenStop()

  private [this] case object Start

}
