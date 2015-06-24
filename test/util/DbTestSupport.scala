package util

import scala.annotation.tailrec

import com.typesafe.config.ConfigFactory
import org.flywaydb.core.Flyway
import org.postgresql.ds.PGSimpleDataSource
import org.scalatest.{BeforeAndAfterAll, Outcome, Suite, SuiteMixin}

trait DbTestSupport extends SuiteMixin with BeforeAndAfterAll { this: Suite ⇒
  import DbTestSupport._
  val api = slick.driver.PostgresDriver.api
  import api._

  implicit lazy val db = database

  override protected def beforeAll(): Unit = {
    if (!migrated) {
      val flyway = new Flyway
      flyway.setDataSource(jdbcDataSourceFromConfig("db.test"))
      flyway.setLocations("filesystem:./sql")

      flyway.clean()
      flyway.migrate()

      migrated = true
    }
  }

  override abstract protected def withFixture(test: NoArgTest): Outcome = {
    val flyway = new Flyway
    flyway.setDataSource(jdbcDataSourceFromConfig("db.test"))
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

    super.withFixture(test)
  }

  def jdbcDataSourceFromConfig(section: String) = {
    val config = ConfigFactory.load
    val source = new PGSimpleDataSource

    source.setServerName(config.getString(s"$section.host"))
    source.setUser(config.getString(s"$section.user"))
    source.setDatabaseName(config.getString(s"$section.name"))

    source
  }
}

object DbTestSupport {
  import slick.driver.PostgresDriver.api.Database

  @volatile var migrated = false

  def database = Database.forConfig("db.test")
}
