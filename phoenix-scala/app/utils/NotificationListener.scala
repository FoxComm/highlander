package utils

import com.github.mauricio.async.db.Configuration
import com.github.mauricio.async.db.postgresql.PostgreSQLConnection
import models.Notification._
import org.postgresql.Driver
import utils.aliases._
import akka.actor._
import akka.stream.actor.ActorPublisherMessage.Cancel

import com.typesafe.scalalogging.LazyLogging
import de.heikoseeberger.akkasse.{ServerSentEvent ⇒ SSE}

object NotificationListener {

  class SSEPublisher extends akka.stream.actor.ActorPublisher[SSE] {

    override def receive: Receive = {
      case msg: SSE ⇒
        onNext(msg)
      case Cancel ⇒ context.stop(self)
      case _      ⇒ ()
    }
  }

  case object NewClientConnected

  protected case class NotifyClients(payload: String)
}

class NotificationListener(adminId: Int, action: (String, ActorRef) ⇒ Unit)(implicit ec: EC)
    extends Actor
    with LazyLogging {

  import NotificationListener._

  override def receive: Receive = {
    case NewClientConnected ⇒
      val child = context.actorOf(akka.actor.Props[SSEPublisher])
      sender() ! child
      context.watch(child)

    case NotifyClients(msg) ⇒
      context.children.foreach { childrenRef ⇒
        action(msg, childrenRef)
      }

    case Terminated(child) ⇒
      context.unwatch(child)
      if (context.children.isEmpty) {
        connection.disconnect
        context.stop(self)
      }
  }

  override def preStart() {
    val myself = self

    connection.connect.map { _ ⇒
      if (connection.isConnected) {
        connection.sendQuery(s"LISTEN ${notificationChannel(adminId)}")
        connection.registerNotifyListener { message ⇒
          myself ! NotifyClients(message.payload)
        }
      } else {
        logger.error("Invalid attempt to start publisher, connection is not active!")
      }
    }
  }

  implicit val formats = JsonFormatters.phoenixFormats

  private lazy val connection = createConnection()

  private def parseUrl(url: String): Configuration = {
    import scala.collection.JavaConverters._

    val emptyProps = new java.util.Properties
    val props      = Driver.parseURL(url, emptyProps).asScala.toMap

    Configuration(username = props("user"),
                  host = props("PGHOST"),
                  port = props("PGPORT").toInt,
                  password = Some(props.getOrElse("password", "")),
                  database = Some(props("PGDBNAME")))
  }

  private def createConnection() = {
    val configuration = parseUrl(FoxConfig.config.db.url)
    new PostgreSQLConnection(configuration)
  }
}
