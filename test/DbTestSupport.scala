import com.typesafe.config.ConfigFactory
import com.wix.accord.{Success => ValidationSuccess, Failure => ValidationFailure }
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

    "LineItemUpdater" - {

      "Adds line_items when the sku doesn't exist in cart" in {
        val cart = Cart(id = 1, accountId = None)
        val payload = Seq[LineItemsPayload](
          LineItemsPayload(skuId = 1, quantity = 3),
          LineItemsPayload(skuId = 2, quantity = 0)
        )

        LineItemUpdater(db, cart, payload).futureValue match {
          case Good(items) =>
            items.filter(_.skuId == 1).length must be(3)
            items.filter(_.skuId == 2).length must be(0)

            val allRecords = db.run(lineItems.result).futureValue

            items must be (allRecords)

          case Bad(s) => fail(s.mkString(";"))
        }
      }

      // TODO: move me to model spec
      "Updates line_items when the Sku already is in cart" in {
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
            items.filter(_.skuId == 1).length must be(3)
            items.filter(_.skuId == 2).length must be(0)
            items.filter(_.skuId == 3).length must be(1)

            val allRecords = db.run(lineItems.result).futureValue

            items must be (allRecords)

          case Bad(s) => fail(s.mkString(";"))
        }
      }
    }

    "Addresses" - {
      val accounts = TableQuery[Users]
      val states = TableQuery[States]
      val addresses = TableQuery[Addresses]

      def seedState(): State = {
        db.run(for {
          stateId <- states += State(0, "Washington", "WA")
          state <- states.filter(_.id === stateId).result.head
        } yield (state)).futureValue
      }

      "can be created" in {
        val state = seedState()

        // 1. validations
        // 2. FK constraints might fail
        val address = db.run(for {
          accountId <- accounts += User(0, "yax@yax.com", "plaintext", "Yax", "Donkey")
          addressId <- addresses += Address(id = 0, accountId = accountId, stateId = state.id, name = "Yax Home",
                                            street1 = "555 E Lake Union St.", street2 = None, city = "Seattle", zip = "90000")
          address <- addresses.filter(_.id === addressId).result.head
        } yield (address)).futureValue

        address.id must be (1)
      }
    }

    "Address" - {
      ".validate" - {
        "returns errors when zip is not 5 digit chars" in {
          val address = Address(id = 0, accountId = 1, stateId = 1, name = "Yax Home",
                                street1 = "555 E Lake Union St.", street2 = None, city = "Seattle", zip = "")
          address.validate match {
            case ValidationFailure(e) =>
              info(e.flatMap(_.description).mkString(";"))

            case ValidationSuccess    => fail("address should invalid")
          }
        }
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
