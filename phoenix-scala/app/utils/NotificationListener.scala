package utils

import com.github.mauricio.async.db.Configuration
import com.github.mauricio.async.db.postgresql.PostgreSQLConnection
import models.Notification._
import org.postgresql.Driver
import utils.aliases._

class NotificationListener(adminId: Int, action: String ⇒ Unit)(implicit ec: EC) {

  implicit val formats = JsonFormatters.phoenixFormats

  private val connection = createConnection()

  connection.connect.map { _ ⇒
    if (connection.isConnected) {
      connection.sendQuery(s"LISTEN ${notificationChannel(adminId)}")
      connection.registerNotifyListener { message ⇒
        action(message.payload)
      }
    } else {
      Console.err.println("Invalid attempt to start publisher, connection is not active!")
    }
  }

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
