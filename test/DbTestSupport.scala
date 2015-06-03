import com.typesafe.config.ConfigFactory
import org.flywaydb.core.Flyway
import org.postgresql.ds.PGSimpleDataSource
import org.scalatest.{MustMatchers, Suite, SuiteMixin, BeforeAndAfterAll, FreeSpec}
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.ExecutionContext

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

class DbTestSupportTest extends FreeSpec
  with MustMatchers
  with DbTestSupport
  with ScalaFutures {

  /** Slick import is still necessary, but this saves you some typing */
  import api._

  implicit val ec: ExecutionContext = concurrent.ExecutionContext.global

  "DB Test Support" - {
    val carts = TableQuery[Carts]
    val findById = carts.findBy(_.id)

    "allows access to the data base" in {
      val insert = carts.returning(carts.map(_.id)) += Cart(0, Some(42))
      val insertedID = db.run(insert).futureValue

      val found = db.run(findById(insertedID).result).futureValue
      found.head.accountId must contain (42)
    }
  }
}
