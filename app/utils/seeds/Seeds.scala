package utils.seeds

import java.time.{Instant, ZoneId}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import com.typesafe.config.Config
import cats.data.Xor
import models.Reason._
import models.inventory._
import models.product.SimpleContext
import models.objects.ObjectContexts
import models.order.{OrderPayment, OrderShippingAddress}
import models.payment.creditcard.CreditCardCharge
import models.{Reason, Reasons}
import org.postgresql.ds.PGSimpleDataSource
import failures.{Failures, FailuresOps}
import slick.driver.PostgresDriver.backend.DatabaseDef
import slick.driver.PostgresDriver.api._
import utils.db._
import utils.db.DbResultT._
import utils.{FoxConfig, JsonFormatters}
import flyway.newFlyway
import models.activity.ActivityContext
import utils.aliases.AC

object Seeds {

  def main(args: Array[String]): Unit = {
    Console.err.println("Cleaning DB and running migrations")
    val config: Config           = FoxConfig.loadWithEnv()
    implicit val db: DatabaseDef = Database.forConfig("db", config)
    implicit val ac: ActivityContext = ActivityContext(
        userId = 1, userType = "admin", transactionId = "seeds")
    flyWayMigrate(config)

    createBaseSeeds()

    val scale = if (args.length == 2) args(1).toInt else 1

    args.headOption.map {
      case "random" ⇒
        createRandomSeeds(scale)
      case "stage" ⇒
        createStageSeeds()
      case "ranking" ⇒
        createRankingSeeds()
      case "demo" ⇒
        createStageSeeds()
        createDemoSeeds()
        createRandomSeeds(scale)
      case _ ⇒ None
    }

    db.close()
  }

  def createBaseSeeds()(implicit db: Database) {
    Console.err.println("Inserting Base Seeds")
    val result: Failures Xor Unit = Await.result(createBase().runTxn(), 4.minutes)
    validateResults("base", result)
  }

  def createStageSeeds()(implicit db: Database, ac: AC) {
    Console.err.println("Inserting Stage seeds")
    val result: Failures Xor Unit = Await.result(createStage().runTxn(), 4.minutes)
    validateResults("stage", result)
  }

  def createDemoSeeds()(implicit db: Database) {
    val result = Await.result(DemoSeeds.insertDemoSeeds.runTxn(), 4.minutes)
    validateResults("demo", result)
  }

  def createRankingSeeds()(implicit db: Database) {
    Console.err.println("Inserting ranking seeds")
    Await.result(RankingSeedsGenerator.insertRankingSeeds(1700).runTxn(), 4.minutes)
  }

  def createRandomSeeds(scale: Int)(implicit db: Database, ac: AC) {
    Console.err.println("Inserting random seeds")

    val customers            = 1000 * scale
    val batchSize            = 100
    val appeasementsPerBatch = 8
    val batchs               = customers / batchSize

    Console.err.println(s"Generating $customers customers in $batchs batches")

    // Have to generate data in batches because of DBIO.seq stack overflow bug.
    // https://github.com/slick/slick/issues/1186
    (1 to batchs).foreach { b ⇒
      Console.err.println(s"Generating random batch $b of $batchSize customers")
      val result = Await.result(
          SeedsGenerator.insertRandomizedSeeds(batchSize, appeasementsPerBatch).runTxn(),
          (120 * scale).second)
      validateResults(s"random batch $b", result)
    }
  }

  val today = Instant.now().atZone(ZoneId.of("UTC"))

  def createBase()(implicit db: Database): DbResultT[Unit] =
    for {
      _       ← * <~ Warehouses.create(Factories.warehouse)
      context ← * <~ ObjectContexts.create(SimpleContext.create())
      admin   ← * <~ Factories.createStoreAdmins
    } yield {}

  def createStage()(implicit db: Database, ac: AC): DbResultT[Unit] =
    for {
      context ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
      ruContext ← * <~ ObjectContexts.create(
                     SimpleContext.create(name = SimpleContext.ru, lang = "ru"))
      admin      ← * <~ Factories.createStoreAdmins
      customers  ← * <~ Factories.createCustomers
      _          ← * <~ Factories.createAddresses(customers)
      _          ← * <~ Factories.createCreditCards(customers)
      products   ← * <~ Factories.createProducts
      ruProducts ← * <~ Factories.createRuProducts(products)
      skus ← * <~ Factories.createInventory(
                Seq(products._1,
                    products._2,
                    products._3,
                    products._4,
                    products._5,
                    products._6,
                    products._7))
      ruSkus ← * <~ Factories.createInventory(
                  Seq(ruProducts._1,
                      ruProducts._2,
                      ruProducts._3,
                      ruProducts._4,
                      ruProducts._5,
                      ruProducts._6,
                      ruProducts._7))
      shipMethods ← * <~ Factories.createShipmentRules
      _           ← * <~ Reasons.createAll(Factories.reasons.map(_.copy(storeAdminId = admin)))
      _           ← * <~ Factories.createGiftCards
      _           ← * <~ Factories.createStoreCredits(admin, customers._1, customers._3)
      orders      ← * <~ Factories.createOrders(customers, products, shipMethods, context)
      _           ← * <~ Factories.createRmas
      // Promotions
      discounts  ← * <~ Factories.createDiscounts
      promotions ← * <~ Factories.createCouponPromotions(discounts)
      coupons    ← * <~ Factories.createCoupons(promotions)
    } yield {}

  object Factories
      extends CustomerSeeds
      with GiftCardSeeds
      with StoreCreditSeeds
      with RmaSeeds
      with ProductSeeds
      with InventorySeeds
      with ShipmentSeeds
      with OrderSeeds
      with StoreAdminSeeds
      with AddressSeeds
      with CreditCardSeeds
      with CustomersGroupSeeds
      with DiscountSeeds
      with PromotionSeeds
      with CouponSeeds {

    implicit val formats = JsonFormatters.phoenixFormats

    def orderPayment = OrderPayment.build(creditCard)

    def giftCardPayment = OrderPayment.build(giftCard)

    def storeCreditPayment = OrderPayment.build(storeCredit)

    def shippingAddress =
      OrderShippingAddress(regionId = 4174,
                           name = "Old Yax",
                           address1 = "9313 Olde Mill Pond Dr",
                           address2 = None,
                           city = "Glen Allen",
                           zip = "23060",
                           phoneNumber = None)

    def creditCardCharge =
      CreditCardCharge(creditCardId = creditCard.id,
                       orderPaymentId = orderPayment.id,
                       chargeId = "foo",
                       amount = 25)

    def reason = Reason(id = 0, storeAdminId = 0, body = "I'm a reason", parentId = None)

    def reasons: Seq[Reason] =
      Seq(
          // Gift card creation reasons
          Reason(body = "Gift to loyal customer",
                 reasonType = GiftCardCreation,
                 parentId = None,
                 storeAdminId = 0),
          Reason(body = "New year GC giveaway",
                 reasonType = GiftCardCreation,
                 parentId = None,
                 storeAdminId = 0),
          // Store credit creation reasons
          Reason(body = "Gift to loyal customer",
                 reasonType = StoreCreditCreation,
                 parentId = None,
                 storeAdminId = 0),
          Reason(body = "New year SC giveaway",
                 reasonType = StoreCreditCreation,
                 parentId = None,
                 storeAdminId = 0),
          // Cancellation reasons
          Reason(body = "Cancelled by customer request",
                 reasonType = Cancellation,
                 parentId = None,
                 storeAdminId = 0),
          Reason(body = "Cancelled because duplication",
                 reasonType = Cancellation,
                 parentId = None,
                 storeAdminId = 0),
          Reason(body = "Other cancellation reason",
                 reasonType = Cancellation,
                 parentId = None,
                 storeAdminId = 0)
      )
  }

  private def flyWayMigrate(config: Config): Unit = {
    val flyway = newFlyway(jdbcDataSourceFromConfig("db", config))

    flyway.clean()
    flyway.migrate()
  }

  private def jdbcDataSourceFromConfig(section: String, config: Config) = {
    val source = new PGSimpleDataSource

    source.setServerName(config.getString(s"$section.host"))
    source.setUser(config.getString(s"$section.user"))
    source.setDatabaseName(config.getString(s"$section.name"))

    source
  }

  private def validateResults(seed: String, result: Failures Xor Unit) {
    result.fold(failures ⇒ {
      Console.err.println(s"Failed generating $seed seeds!")
      failures.flatten.foreach(Console.err.println)
      System.exit(1)
    }, _ ⇒ Console.err.println(s"Successfully created $seed seeds!"))
  }
}
