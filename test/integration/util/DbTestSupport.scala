package util

import java.util.Locale
import javax.sql.DataSource

import scala.annotation.tailrec

import cats.data.Xor
import utils.flyway.newFlyway
import org.scalatest.{BeforeAndAfterAll, Outcome, Suite, SuiteMixin}
import services.Failures
import slick.jdbc.hikaricp.HikariCPJdbcDataSource
import slick.driver.PostgresDriver.api.Database
import java.sql.Connection
import util.SlickSupport.implicits._
import slick.jdbc.DriverDataSource

import models.product.{SimpleContext, ProductContext, ProductContexts}
import scala.concurrent.ExecutionContext.Implicits.global

trait DbTestSupport extends SuiteMixin with BeforeAndAfterAll { this: Suite ⇒
  import DbTestSupport._
  val api = slick.driver.PostgresDriver.api

  implicit lazy val db = database

  /* tables which should *not* be truncated b/c they're static and seeded by migration */
  val doNotTruncate = Set("states", "countries", "regions", "schema_version")

  override protected def beforeAll(): Unit = {
    if (!migrated) {
      Locale.setDefault(Locale.US)
      val ds = new DriverDataSource(TestBase.config.getString("db.url"))
      val flyway = newFlyway(ds)

      flyway.clean()
      flyway.migrate()

      ds.close()
      migrated = true
    }
    setupProductContext()
  }

  private def setupProductContext(): Failures Xor ProductContext = {
    ProductContexts.create(SimpleContext.create).futureValue
  }

  def isTableEmpty(table: String)(implicit conn: Connection): Boolean = {
    val stmt = conn.createStatement()
    val rs = stmt.executeQuery(s"select true from $table limit 1")
    !rs.isBeforeFirst
  }

  override abstract protected def withFixture(test: NoArgTest): Outcome = {
    implicit val conn      = jdbcDataSourceFromSlickDB(db).getConnection

    val config    = conn.getMetaData
    val allTables = conn.getMetaData.getTables(conn.getCatalog, "public", "%", Array("TABLE"))

    @tailrec
    def iterate(in: Seq[String]): Seq[String] = {
      if (allTables.next()) {
        iterate(in :+ allTables.getString(3))
      } else {
        in
      }
    }

    val tables = iterate(Seq()).filterNot {
      t ⇒ t.startsWith("pg_") || doNotTruncate.contains(t) || isTableEmpty(t)
    }

    if (tables.nonEmpty) {
      conn.createStatement().execute(s"truncate ${tables.mkString(", ")} restart identity cascade;")
    }
    setupProductContext()
    conn.close()

    super.withFixture(test)
  }

  def jdbcDataSourceFromSlickDB(db: api.Database): DataSource = db.source match {
    case source: HikariCPJdbcDataSource ⇒ source.ds
  }
}

object DbTestSupport {

  @volatile var migrated = false

  lazy val database = Database.forConfig("db", TestBase.config)
}
