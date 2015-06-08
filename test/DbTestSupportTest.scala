import com.typesafe.config.ConfigFactory
import com.wix.accord.{Success => ValidationSuccess, Failure => ValidationFailure }
import org.flywaydb.core.Flyway
import org.postgresql.ds.PGSimpleDataSource
import org.scalactic.{Good, Bad}
import org.scalatest.{MustMatchers, Suite, SuiteMixin, BeforeAndAfterAll, FreeSpec}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.prop.TableDrivenPropertyChecks._
import scala.concurrent.ExecutionContext

trait DbTestSupport extends SuiteMixin with BeforeAndAfterAll { this: Suite ⇒
  val api = slick.driver.PostgresDriver.api
  import api._

  lazy val db = Database.forConfig("db.test")

  implicit val implicitDB = db

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
  }
}
