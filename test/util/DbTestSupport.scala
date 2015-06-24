package util

import scala.annotation.tailrec

import com.typesafe.config.ConfigFactory
import org.flywaydb.core.Flyway
import org.postgresql.ds.PGSimpleDataSource
import org.scalatest.{BeforeAndAfterAll, Outcome, Suite, SuiteMixin}

trait DbTestSupport extends SuiteMixin with BeforeAndAfterAll { this: Suite ⇒
  val api = slick.driver.PostgresDriver.api
  import api._

  implicit lazy val db = Database.forConfig("db.test")

  override protected def beforeAll(): Unit = {
    val flyway = new Flyway
    flyway.setDataSource(jdbcDataSourceFromConfig("db.test"))
    flyway.setLocations("filesystem:./sql")

    flyway.clean()
    flyway.migrate()
  }

  override abstract protected def withFixture(test: NoArgTest): Outcome = {
    val config  = jdbcDataSourceFromConfig("db.test")

    val conn     = config.getConnection
    val metaData = conn.getMetaData
    val catalog  = conn.getCatalog
    val tables   = metaData.getTables(catalog, "public", "%", Array("TABLE"))

    @tailrec
    def iterate(in: Seq[String]): Seq[String] = {
      if (tables.next()) {
        iterate(in :+ tables.getString(3))
      } else {
        in
      }
    }

    println(iterate(Seq.empty[String]).filterNot(_.startsWith("pg_")))

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

}
