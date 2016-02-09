package utils.seeds

import java.time.{Instant, ZoneId}

import cats.data.Xor
import models.Reason._
import models.{CreditCardCharge, OrderPayment, OrderShippingAddress, Reason, Reasons}
import org.postgresql.ds.PGSimpleDataSource
import services.{Failures, FailuresOps}
import slick.driver.PostgresDriver
import slick.driver.PostgresDriver.api._
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.JsonFormatters
import utils.flyway.newFlyway
import utils.Slick.implicits._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._



object Seeds {

  def main(args: Array[String]): Unit = {
    Console.err.println(s"Cleaning DB and running migrations")
    val config: com.typesafe.config.Config = utils.Config.loadWithEnv()
    implicit val db: PostgresDriver.backend.DatabaseDef = Database.forConfig("db", config)
    flyWayMigrate(config)

    createBaseSeeds()

    args.headOption.map {
      case "random" ⇒  createRandomSeeds()
      case "ranking" ⇒ createRankingSeeds()
      case "demo" ⇒  { 
        createDemoSeeds()
        createRandomSeeds()
      }
      case _ ⇒ None
    }

    db.close()
  }

  def createBaseSeeds()(implicit db: Database) {
    Console.err.println(s"Inserting seeds")
    val result: Failures Xor Unit = Await.result(createAll().runTxn(), 20.second)
    validateResults("base", result)
  }

  def createDemoSeeds()(implicit db: Database) {
    val result: Failures Xor Unit = Await.result(DemoSeeds.insertDemoSeeds.runTxn(), 120.seconds)
      validateResults("demo", result)
  }

  def createRankingSeeds()(implicit db: Database) {
    Console.err.println(s"Inserting ranking seeds")
    Await.result(db.run(RankingSeedsGenerator.insertRankingSeeds(1700).transactionally), 30.second)
  }

  def createRandomSeeds()(implicit db: Database) {
    Console.err.println(s"Inserting random seeds")
    val customers = 1000
    val batchSize = 100 
    val batchs = customers / batchSize
    val productsPerBatch = 20
    //Have to generate data in batches because of DBIO.seq stack overflow bug.
    //https://github.com/slick/slick/issues/1186
    (1 to batchs) map { b ⇒ 
      Console.err.println(s"Generating random batch $b of $batchSize customers")
      val result: Failures Xor Unit = Await.result(
        SeedsGenerator.insertRandomizedSeeds(batchSize, productsPerBatch).runTxn(), 120.second)
      validateResults("random", result)
    }
  }

  val today = Instant.now().atZone(ZoneId.of("UTC"))

  def createAll()(implicit db: Database): DbResultT[Unit] = for {
    admin ← * <~ Factories.createStoreAdmins
    customers ← * <~ Factories.createCustomers
    _ ← * <~ Factories.createAddresses(customers)
    _ ← * <~ Factories.createCreditCards(customers)
    skus ← * <~ Factories.createInventory
    shipMethods ← * <~ Factories.createShipmentRules
    _ ← * <~ Reasons.createAll(Factories.reasons.map(_.copy(storeAdminId = admin)))
    _ ← * <~ Factories.createGiftCards
    _ ← * <~ Factories.createStoreCredits(admin, customers._1, customers._3)
    orders ← * <~ Factories.createOrders(customers, skus, shipMethods)
    _ ← * <~ Factories.createRmas
  } yield {}

  object Factories extends CustomerSeeds with GiftCardSeeds with StoreCreditSeeds with RmaSeeds with InventorySeeds
  with ShipmentSeeds with OrderSeeds with StoreAdminSeeds with AddressSeeds with CreditCardSeeds
  with CustomersGroupSeeds {

    implicit val formats = JsonFormatters.phoenixFormats

    def orderPayment = OrderPayment.build(creditCard)

    def giftCardPayment = OrderPayment.build(giftCard)

    def storeCreditPayment = OrderPayment.build(storeCredit)

    def shippingAddress = OrderShippingAddress(regionId = 4174, name = "Old Yax", address1 = "9313 Olde Mill Pond Dr",
      address2 = None, city = "Glen Allen", zip = "23060", phoneNumber = None)

    def creditCardCharge = CreditCardCharge(creditCardId = creditCard.id, orderPaymentId = orderPayment.id,
      chargeId = "foo", amount = 25)

    def reason = Reason(id = 0, storeAdminId = 0, body = "I'm a reason", parentId = None)

    def reasons: Seq[Reason] = Seq(
      // Gift card creation reasons
      Reason(body = "Gift to loyal customer", reasonType = GiftCardCreation, parentId = None, storeAdminId = 0),
      Reason(body = "New year GC giveaway", reasonType = GiftCardCreation, parentId = None, storeAdminId = 0),
      // Store credit creation reasons
      Reason(body = "Gift to loyal customer", reasonType = StoreCreditCreation, parentId = None, storeAdminId = 0),
      Reason(body = "New year SC giveaway", reasonType = StoreCreditCreation, parentId = None, storeAdminId = 0),
      // Cancellation reasons
      Reason(body = "Cancelled by customer request", reasonType = Cancellation, parentId = None, storeAdminId = 0),
      Reason(body = "Cancelled because duplication", reasonType = Cancellation, parentId = None, storeAdminId = 0),
      Reason(body = "Other cancellation reason", reasonType = Cancellation, parentId = None, storeAdminId = 0)
    )
  }

  private def flyWayMigrate(config: com.typesafe.config.Config): Unit = {
    val flyway = newFlyway(jdbcDataSourceFromConfig("db", config))

    flyway.clean()
    flyway.migrate()
  }

  private def jdbcDataSourceFromConfig(section: String, config: com.typesafe.config.Config) = {
    val source = new PGSimpleDataSource

    source.setServerName(config.getString(s"$section.host"))
    source.setUser(config.getString(s"$section.user"))
    source.setDatabaseName(config.getString(s"$section.name"))

    source

  }

  private def validateResults(seed: String, result: Failures Xor Unit) {
    result.fold(failures ⇒ {
      Console.err.println(s"Failed generating $seed seeds")
      failures.flatten.foreach(f⇒  { 
        Console.err.println(f)
      })
    },
    _ ⇒ Console.err.println("Success!"))
  }

}
