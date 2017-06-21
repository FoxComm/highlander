package phoenix.utils

import akka.actor._

import com.github.mauricio.async.db.Configuration
import com.github.mauricio.async.db.postgresql.PostgreSQLConnection
import com.typesafe.scalalogging.LazyLogging
import de.heikoseeberger.akkasse.scaladsl.model.{ServerSentEvent ⇒ SSE}
import org.postgresql.Driver
import phoenix.models.Notification._
import phoenix.utils.aliases._
import slick.jdbc.hikaricp.HikariCPJdbcDataSource

object NotificationListener extends LazyLogging {
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

  private def getDbUrl()(implicit db: DB): String =
    db.source match {
      case source: HikariCPJdbcDataSource ⇒ source.hconf.getJdbcUrl
      case _                              ⇒ FoxConfig.config.db.url
    }

  private def createConnection()(implicit db: DB): PostgreSQLConnection = {
    val configuration = parseUrl(getDbUrl)
    new PostgreSQLConnection(configuration)
  }

  def props(accountId: Int, actorSource: ActorRef)(implicit ec: EC, db: DB): Props =
    Props(new NotificationListener(accountId, actorSource))

}

class NotificationListener(adminId: Int, actorSource: ActorRef)(implicit ec: EC, db: DB)
    extends Actor
    with LazyLogging {
  import NotificationListener._

  implicit val formats = JsonFormatters.phoenixFormats

  private lazy val connection = createConnection()

  override def receive: Receive = {
    case Terminated(child) ⇒
      context.unwatch(child)
      connection.disconnect
      context.stop(self)
  }

  override def preStart(): Unit = {
    context.watch(actorSource)

    connection.connect.map { _ ⇒
      if (connection.isConnected) {
        connection.sendQuery(s"LISTEN ${notificationChannel(adminId)}")
        connection.registerNotifyListener { message ⇒
          actorSource ! SSE(message.payload)
        }
      } else {
        actorSource ! akka.actor.Status
          .Failure(new RuntimeException("Invalid attempt to start publisher, connection is not active!"))
        logger.error("Invalid attempt to start publisher, connection is not active!")
      }
    }
  }

  override def postStop(): Unit = connection.disconnect
}
