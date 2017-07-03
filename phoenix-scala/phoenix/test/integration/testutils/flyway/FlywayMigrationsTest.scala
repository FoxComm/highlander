package testutils.flyway

import org.flywaydb.core.api.{MigrationInfo, MigrationVersion}
import org.flywaydb.core.api.callback.BaseFlywayCallback
import testutils.{GimmeSupport, TestBase}
import java.sql.Connection
import slick.jdbc.PostgresProfile.api._
import com.typesafe.scalalogging.LazyLogging
import slick.jdbc.JdbcBackend.{BaseSession, DatabaseDef}
import slick.jdbc._
import slick.util.AsyncExecutor

import scala.concurrent.ExecutionContextExecutor
import scala.io.Source

abstract class FlywayMigrationsTest(migrationVersion: String)
    extends BaseFlywayCallback
    with GimmeSupport
    with TestBase
    with LazyLogging {

  implicit val ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.Implicits.global

  /*
    Defines target version that needs to be tested
   */
  val versionToTest: MigrationVersion = MigrationVersion.fromVersion(migrationVersion)

  override def beforeEachMigrate(connection: Connection, info: MigrationInfo) =
    if (info.getVersion == versionToTest) {
      logger.info(s"beforeEachMigrate ${info.getVersion}")
      testBeforeMigration(new UnmanagedConnection(connection))
    }

  override def afterEachMigrate(connection: Connection, info: MigrationInfo) =
    if (info.getVersion == versionToTest) {
      logger.info(s"afterEachMigrate ${info.getVersion}")
      testAfterMigration(new UnmanagedConnection(connection))
    }

  /*
    Do not throw any exception inside, it will cause transaction to rollback and any following sql will crash
   */
  def testBeforeMigration(implicit db: Database): Unit
  def testAfterMigration(implicit db: Database): Unit

  def runSql(fileName: String)(implicit db: Database): Unit = {
    val sqlQuery =
      Source.fromInputStream(getClass.getResourceAsStream(fileName)).mkString
    SQLActionBuilder(sqlQuery, BlankParameter).asUpdate.gimme
  }

  case object BlankParameter extends SetParameter[Unit] {
    override def apply(v1: Unit, v2: PositionedParameters): Unit = {}
  }
}

object FlywayMigrationsTest {
  def list: Seq[FlywayMigrationsTest] = Seq(
    PluginsMigrationTest,
    AddressesMigrationTest
  )
}

//  JDBC wrappers

class UnmanagedJdbcDataSource(conn: Connection) extends JdbcDataSource {
  def createConnection()                   = conn
  def close()                              = ()
  override val maxConnections: Option[Int] = Some(5)
}

class UnmanagedSession(database: DatabaseDef) extends BaseSession(database) {
  override def close() = ()
}

class UnmanagedConnection(conn: Connection)
    extends JdbcBackend.DatabaseDef(new UnmanagedJdbcDataSource(conn), AsyncExecutor("AsyncExecutor", 1, -1)) {
  override def createSession() = new UnmanagedSession(this)
}
