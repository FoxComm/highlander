package util

import com.typesafe.config.ConfigFactory
import org.flywaydb.core.Flyway
import org.postgresql.ds.PGSimpleDataSource
import org.scalatest.{BeforeAndAfterAll, Suite, SuiteMixin}

trait DbTestSupport extends SuiteMixin with BeforeAndAfterAll { this: Suite ⇒
  val api = slick.driver.PostgresDriver.api
  import api._

  lazy val db = Database.forConfig("db.test")

  override protected def beforeAll(): Unit = {
    super.beforeAll()

    val flyway = new Flyway
    flyway.setDataSource(jdbcDataSourceFromConfig("db.test"))
    flyway.setLocations("filesystem:./sql")
    flyway.clean()
    flyway.migrate()
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
