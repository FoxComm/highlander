package testutils

import java.sql.Connection
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

  val api = slick.driver.PostgresDriver.api

  implicit lazy val db = database

  implicit val ec: EC

  /* tables which should *not* be truncated b/c they're static and seeded by migration */
  val doNotTruncate = Set("states", "countries", "regions", "schema_version")

  override protected def beforeAll(): Unit = {
    if (!migrated) {
      Locale.setDefault(Locale.US)
      val db4fly = Database.forConfig("db", TestBase.config)
      val flyway = newFlyway(jdbcDataSourceFromSlickDB(db4fly))

      flyway.clean()
      flyway.migrate()

      db4fly.close()

      migrated = true
    }
  }

  private def setupObjectContext(): ObjectContext =
    ObjectContexts.create(SimpleContext.create()).gimme

  def isTableEmpty(table: String)(implicit conn: Connection): Boolean = {
    val stmt = conn.createStatement()
    val rs   = stmt.executeQuery(s"select true from $table limit 1")
    !rs.isBeforeFirst
  }

  override abstract protected def withFixture(test: NoArgTest): Outcome = {
    implicit val conn = jdbcDataSourceFromSlickDB(db).getConnection

    val allTables = conn.getMetaData.getTables(conn.getCatalog, "public", "%", Array("TABLE"))

    @tailrec
    def iterate(in: Seq[String]): Seq[String] = {
      if (allTables.next()) {
        iterate(in :+ allTables.getString(3))
      } else {
        in
      }
    }

    val tables = iterate(Seq()).filterNot { t ⇒
      t.startsWith("pg_") || doNotTruncate.contains(t) || isTableEmpty(t)
    }

    if (tables.nonEmpty) {
      conn
        .createStatement()
        .execute(s"truncate ${tables.mkString(", ")} restart identity cascade;")
    }

    setupObjectContext()
    Seeds.createSingleMerchantSystem.gimme

    conn.close()

    super.withFixture(test)
  }

  def jdbcDataSourceFromSlickDB(db: api.Database): DataSource =
    db.source match {
      case source: HikariCPJdbcDataSource ⇒ source.ds
    }
}

object DbTestSupport {

  @volatile var migrated = false

  lazy val database = Database.forConfig("db", TestBase.config)
}
