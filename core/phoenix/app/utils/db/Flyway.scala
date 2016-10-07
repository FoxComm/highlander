package utils.db

import javax.sql.DataSource

import org.flywaydb.core.Flyway
import org.flywaydb.core.internal.util.logging.{LogCreator, LogFactory}

object flyway {

  def newFlyway(dataSource: DataSource, locations: Seq[String] = Seq("filesystem:./sql")): Flyway = {
    LogFactory.setLogCreator(FlywayLogCreator)

    val flyway = new Flyway
    flyway.setDataSource(dataSource)
    flyway.setSchemas("public")
    flyway.setLocations(locations: _*)
    flyway
  }

  private object FlywayLogCreator extends LogCreator {
    def createLogger(clazz: Class[_]) = FlywayLog
  }

  private object FlywayLog extends org.flywaydb.core.internal.util.logging.Log {
    def debug(message: String) = {}
    def info(message: String)  = {}
    def warn(message: String)  = {}
    def error(message: String) = { Console.err.println(message) }
    def error(message: String, e: Exception) = {
      Console.err.println(message)
      Console.err.println(e.getMessage)
    }
  }
}
