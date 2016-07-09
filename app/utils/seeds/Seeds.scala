package utils.seeds

import java.time.{Instant, ZoneId}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import cats.data.Xor
import com.typesafe.config.Config
import failures.{Failures, FailuresOps}
import models.Reason._
import models.activity.ActivityContext
import models.cord.{OrderPayment, OrderShippingAddress}
import models.objects.ObjectContexts
import models.payment.creditcard.CreditCardCharge
import models.product.SimpleContext
import models.{Reason, Reasons}
import org.postgresql.ds.PGSimpleDataSource
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.DatabaseDef
import utils.aliases._
import utils.db._
import utils.db.flyway.newFlyway
import utils.{FoxConfig, JsonFormatters}

object Seeds {

  def main(args: Array[String]): Unit = {
    Console.err.println("Cleaning DB and running migrations")
    val config: Config           = FoxConfig.loadWithEnv()
    implicit val db: DatabaseDef = Database.forConfig("db", config)
    implicit val ac: AC          = ActivityContext(userId = 1, userType = "admin", transactionId = "seeds")
    flyWayMigrate(config)

    val adminId = createBaseSeeds()

    args.headOption.map {
      case "stage" ⇒
        createStageSeeds(adminId)
      case _ ⇒ None
    }

    db.close()
  }

  def createBaseSeeds()(implicit db: DB): Int = {
    Console.err.println("Inserting Base Seeds")
    val result: Failures Xor Int = Await.result(createBase().runTxn(), 4.minutes)
    validateResults("base", result)
  }

  def createStageSeeds(adminId: Int)(implicit db: DB, ac: AC) {
    Console.err.println("Inserting Stage seeds")
    val result: Failures Xor Unit = Await.result(createStage(adminId).runTxn(), 4.minutes)
    validateResults("stage", result)
  }

  val today = Instant.now().atZone(ZoneId.of("UTC"))

  def createBase()(implicit db: DB): DbResultT[Int] =
    for {
      context ← * <~ ObjectContexts.create(SimpleContext.create())
      admin   ← * <~ Factories.createStoreAdmins
    } yield admin

  def createStage(adminId: Int)(implicit db: DB, ac: AC): DbResultT[Unit] =
    for {
      context ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
      ruContext ← * <~ ObjectContexts.create(
                     SimpleContext.create(name = SimpleContext.ru, lang = "ru"))
      customers   ← * <~ Factories.createCustomers
      _           ← * <~ Factories.createAddresses(customers)
      _           ← * <~ Factories.createCreditCards(customers)
      products    ← * <~ Factories.createProducts
      ruProducts  ← * <~ Factories.createRuProducts(products)
      shipMethods ← * <~ Factories.createShipmentRules
      _           ← * <~ Reasons.createAll(Factories.reasons.map(_.copy(storeAdminId = adminId)))
      _           ← * <~ Factories.createGiftCards
      _           ← * <~ Factories.createStoreCredits(adminId, customers._1, customers._3)
      // Promotions
      search     ← * <~ Factories.createSharedSearches(adminId)
      discounts  ← * <~ Factories.createDiscounts(search)
      promotions ← * <~ Factories.createCouponPromotions(discounts)
      coupons    ← * <~ Factories.createCoupons(promotions)
    } yield {}

  object Factories
      extends CustomerSeeds
      with GiftCardSeeds
      with StoreCreditSeeds
      with ReturnSeeds
      with ProductSeeds
      with ShipmentSeeds
      with OrderSeeds
      with StoreAdminSeeds
      with AddressSeeds
      with CreditCardSeeds
      with CustomersGroupSeeds
      with DiscountSeeds
      with PromotionSeeds
      with CouponSeeds
      with SharedSearchSeeds {

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

  private def validateResults[R](seed: String, result: Failures Xor R)(implicit db: DB): R = {
    result.fold(failures ⇒ {
      Console.err.println(s"Failed generating $seed seeds!")
      failures.flatten.foreach(Console.err.println)
      db.close()
      sys.exit(1)
    }, v ⇒ { Console.err.println(s"Successfully created $seed seeds!"); v })
  }
}
