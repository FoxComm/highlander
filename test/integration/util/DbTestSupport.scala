package util

import javax.sql.DataSource

import scala.annotation.tailrec

import org.flywaydb.core.Flyway
import org.scalatest.{BeforeAndAfterAll, Outcome, Suite, SuiteMixin}
import slick.jdbc.HikariCPJdbcDataSource

trait DbTestSupport extends SuiteMixin with BeforeAndAfterAll { this: Suite ⇒
  import DbTestSupport._
  val api = slick.driver.PostgresDriver.api

  implicit lazy val db = database

  override protected def beforeAll(): Unit = {
    if (!migrated) {
      val flyway = new Flyway
      flyway.setDataSource(jdbcDataSourceFromSlickDB(db))
      flyway.setLocations("filesystem:./sql")

      flyway.clean()
      flyway.migrate()

      migrated = true
    }
  }

  override abstract protected def withFixture(test: NoArgTest): Outcome = {
    val flyway = new Flyway
    flyway.setDataSource(jdbcDataSourceFromSlickDB(db))
    flyway.setLocations("filesystem:./sql")

    val conn      = flyway.getDataSource.getConnection
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

    val tables = iterate(Seq()).filterNot { t ⇒ t.startsWith("pg_") || t == "states" }

    conn.createStatement().execute(s"truncate ${tables.mkString(", ")} restart identity cascade;")
    conn.close()

    super.withFixture(test)
  }

  def jdbcDataSourceFromSlickDB(db: api.Database): DataSource = db.source match {
    case source: HikariCPJdbcDataSource ⇒ source.ds
  }
}

object DbTestSupport {
  import slick.driver.PostgresDriver.api.Database

  @volatile var migrated = false

  lazy val database = Database.forConfig("db", TestBase.config)
}
