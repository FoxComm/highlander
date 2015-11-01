package utils

import org.flywaydb.core.Flyway
import org.flywaydb.core.api.callback
import java.sql.Connection
import javax.sql.DataSource
import org.flywaydb.core.api.MigrationInfo

object flyway {

  def newFlyway(dataSource: DataSource,
        locations: Seq[String] = Seq("filesystem:./sql"),
        callbacks: Seq[FlywayCallbacks] = Seq(CleanCallback)): Flyway = {
    val flyway = new Flyway
    flyway.setDataSource(dataSource)
    flyway.setLocations(locations: _*)
    flyway.setCallbacks(callbacks: _*)
    flyway
  }

  case object CleanCallback extends FlywayCallbacks {
    override def beforeClean(conn: Connection) = {
      val stmt = conn.createStatement()
      stmt.execute("drop extension if exists pg_trgm cascade")
    }
  }

  trait FlywayCallbacks extends callback.FlywayCallback {
    def beforeClean(connection: Connection): Unit = {}
    def afterClean(connection: Connection): Unit = {}

    def beforeMigrate(connection: Connection): Unit = {}
    def afterMigrate(connection: Connection): Unit = {}

    def beforeEachMigrate(connection: Connection, info: MigrationInfo): Unit = {}
    def afterEachMigrate(connection: Connection, info: MigrationInfo): Unit = {}

    def beforeValidate(connection: Connection): Unit = {}
    def afterValidate(connection: Connection): Unit = {}

    def beforeBaseline(connection: Connection): Unit = {}
    def afterBaseline(connection: Connection): Unit = {}

    def beforeRepair(connection: Connection): Unit = {}
    def afterRepair(connection: Connection): Unit = {}

    def beforeInfo(connection: Connection): Unit = {}
    def afterInfo(connection: Connection): Unit = {}

    @deprecated("Will be removed in Flyway 4.0. Use beforeBaseline() instead.", "3.2.1")
    def beforeInit(connection: Connection): Unit = {}
    @deprecated("Will be removed in Flyway 4.0. Use afterBaseline() instead.", "3.2.1")
    def afterInit(connection: Connection): Unit = {}
  }
}