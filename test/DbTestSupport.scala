import com.typesafe.config.ConfigFactory
import org.flywaydb.core.Flyway
import org.postgresql.ds.PGSimpleDataSource
import org.scalactic.{Good, Bad}
import org.scalatest.{MustMatchers, Suite, SuiteMixin, BeforeAndAfterAll, FreeSpec}
import org.scalatest.concurrent.ScalaFutures
import slick.lifted.Tag
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

  val lineItems = TableQuery[LineItems]
  val carts = TableQuery[Carts]

  def createLineItems(items: Seq[LineItem]): Unit = {
    val insert = lineItems ++= items
    db.run(insert).futureValue
  }

  "DB Test Support" - {

    // TODO: move me to model spec
    "LineItemUpdater" in {
      val seedItems = Seq(1, 1, 1, 1, 1, 1, 2, 3, 3).map { skuId => LineItem(id = 0, cartId = 1, skuId = skuId) }
      createLineItems(seedItems)

      val cart = Cart(id = 1, accountId = None)
      val payload = Seq[LineItemsPayload](
        LineItemsPayload(skuId = 1, quantity = 3),
        LineItemsPayload(skuId = 2, quantity = 0),
        LineItemsPayload(skuId = 3, quantity = 1)
      )

      LineItemUpdater(db, cart, payload).futureValue match {
        case Good(items) =>
          items.filter(_.skuId == 1).length must be (3)
          items.filter(_.skuId == 2).length must be (0)
          items.filter(_.skuId == 3).length must be (1)

        case Bad(s) => fail(s.mkString(";"))
      }
    }

    "allows access to the data base" in {
      val findById = carts.findBy(_.id)
      val insert = carts.returning(carts.map(_.id)) += Cart(0, Some(42))
      val insertedID = db.run(insert).futureValue

      val found = db.run(findById(insertedID).result).futureValue
      found.head.accountId must contain (42)
    }
  }
}
