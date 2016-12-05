package testutils

import java.sql.{Connection, PreparedStatement}
import java.util.Locale
import javax.sql.DataSource

import scala.annotation.tailrec

import models.objects.{ObjectContext, ObjectContexts}
import models.product.SimpleContext
import org.scalatest.{BeforeAndAfterAll, Outcome, Suite, SuiteMixin}
import slick.driver.PostgresDriver.api._
import slick.jdbc.hikaricp.HikariCPJdbcDataSource
import utils.aliases.EC
import utils.db.flyway.newFlyway
import utils.seeds.Seeds

trait DbTestSupport extends SuiteMixin with BeforeAndAfterAll with GimmeSupport {
  this: Suite ⇒

  import DbTestSupport._

  implicit lazy val db = database

  implicit val ec: EC

  val api = slick.driver.PostgresDriver.api

  /* tables which should *not* be truncated b/c they're static and seeded by migration */
  val doNotTruncate = Set("states",
                          "countries",
                          "regions",
                          "schema_version",
                          "object_contexts",
                          "systems",
                          "resources",
                          "scopes",
                          "organizations",
                          "scope_domains",
                          "roles",
                          "permissions",
                          "role_permissions")

  override protected def beforeAll(): Unit = {
    if (!migrated) {
      Locale.setDefault(Locale.US)
      val flyway = newFlyway(dataSource)

      flyway.clean()
      flyway.migrate()

      setupObjectContext()
      Seeds.createSingleMerchantSystem.gimme

      val allTables =
        persistConn.getMetaData.getTables(persistConn.getCatalog, "public", "%", Array("TABLE"))

      @tailrec
      def iterate(in: Seq[String]): Seq[String] = {
        if (allTables.next()) {
          iterate(in :+ allTables.getString(3))
        } else {
          in
        }
      }

      tables = iterate(Seq()).filterNot { t ⇒
        t.startsWith("pg_") || t.startsWith("sql_") || doNotTruncate.contains(t)
      }
      val sqlTables = tables.mkString("{", ",", "}")
      nonEmptyTableStmt =
        persistConn.prepareStatement(s"select filter_empty_tables('$sqlTables'::text[]) as tables")

      migrated = true
    }
  }

  private def setupObjectContext(): ObjectContext =
    ObjectContexts.create(SimpleContext.create()).gimme

  private def filterEmptyTables(): Seq[String] = {
    val rs = nonEmptyTableStmt.executeQuery()
    if (rs.next()) {
      rs.getArray("tables") match {
        case null  ⇒ Seq.empty[String]
        case array ⇒ array.getArray.asInstanceOf[scala.Array[String]]
      }
    } else
      Seq.empty[String]
  }

  override abstract protected def withFixture(test: NoArgTest): Outcome = {
    val nonEmptyTables = filterEmptyTables()

    if (nonEmptyTables.nonEmpty) {
      persistConn
        .createStatement()
        .execute(s"truncate ${nonEmptyTables.mkString(", ")} restart identity cascade;")
    }

    super.withFixture(test)
  }
}

object DbTestSupport {

  @volatile var migrated                             = false
  @volatile var tables: Seq[String]                  = Seq()
  @volatile var nonEmptyTableStmt: PreparedStatement = _

  lazy val database    = Database.forConfig("db", TestBase.config)
  lazy val dataSource  = jdbcDataSourceFromSlickDB(database)
  lazy val persistConn = dataSource.getConnection
  val api              = slick.driver.PostgresDriver.api

  def jdbcDataSourceFromSlickDB(db: api.Database): DataSource =
    db.source match {
      case source: HikariCPJdbcDataSource ⇒ source.ds
    }
}
