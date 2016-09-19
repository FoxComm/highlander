package utils.seeds

import java.time.{Instant, ZoneId}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import cats.data.Xor
import com.pellucid.sealerate
import com.typesafe.config.Config
import failures.{Failures, FailuresOps, NotFoundFailure404}
import failures.UserFailures._

import models.Reason._
import models.activity.ActivityContext
import models.cord.{OrderPayment, OrderShippingAddress}
import models.objects.ObjectContexts
import models.payment.creditcard.CreditCardCharge
import models.product.SimpleContext
import models.{Reason, Reasons}
import models.account._
import org.postgresql.ds.PGSimpleDataSource
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.DatabaseDef
import utils.aliases._
import utils.db._
import utils.db.flyway.newFlyway
import utils.seeds.generators.SeedsGenerator
import utils.{ADT, FoxConfig, JsonFormatters}

object Seeds {

  sealed trait Command
  case object NoCommand   extends Command
  case object CreateAdmin extends Command
  case object Seed        extends Command

  object Command extends ADT[Command] {
    def types = sealerate.values[Command]
  }

  case class CliConfig(
      migrateDb: Boolean = true,
      seedBase: Boolean = true,
      seedAdmins: Boolean = false,
      seedAdmin: Boolean = false,
      seedRandom: Int = 0,
      seedStage: Boolean = false,
      seedDemo: Int = 0,
      customersScaleMultiplier: Int = 1000,
      mode: Command = NoCommand,
      adminName: String = "",
      adminEmail: String = "",
      adminOrg: String = "",
      adminRoles: List[String] = List.empty
  )

  def main(args: Array[String]): Unit = {
    val parser = new scopt.OptionParser[CliConfig]("phoenix") {
      head("phoenix", "1.0")

      cmd("seed")
        .action((_, c) ⇒ c.copy(mode = Seed))
        .text("Insert seeds")
        .children(
            opt[Unit]("skipMigrate")
              .action((_, c) ⇒ c.copy(migrateDb = false))
              .text("Skip migration step for database."),
            opt[Unit]("skipBase")
              .action((x, c) ⇒ c.copy(seedBase = false))
              .text("Skip seed base seeds."),
            opt[Unit]("seedAdmins")
              .action((_, c) ⇒ c.copy(seedAdmins = true))
              .text("Create predefined admins"),
            opt[Int]("seedRandom")
              .action((x, c) ⇒ c.copy(seedRandom = x))
              .text("Create random seeds"),
            opt[Int]("customersScaleMultiplier")
              .action((x, c) ⇒ c.copy(customersScaleMultiplier = x))
              .text("Customers scale multiplier for random seeds"),
            opt[Unit]("seedStage")
              .action((x, c) ⇒ c.copy(seedStage = true))
              .text("Create stage seeds"),
            opt[Int]("seedDemo").action((x, c) ⇒ c.copy(seedDemo = x)).text("Create demo seeds"))

      cmd("createAdmin")
        .action((_, c) ⇒ c.copy(mode = CreateAdmin))
        .text(
            "Create Admin. Password prompts via stdin or can be set via admin_password env or prop")
        .children(
            opt[String]("name")
              .required()
              .action((x, c) ⇒ c.copy(adminName = x))
              .text("Admin name"),
            opt[String]("email")
              .required()
              .action((x, c) ⇒ c.copy(adminEmail = x))
              .text("Admin email")
        )
    }

    parser.parse(args, CliConfig()) match {
      case Some(cfg) ⇒
        runMain(cfg, parser.usage)
      case None ⇒
        sys.exit(1)
    }
  }

  def runMain(cfg: CliConfig, usage: String): Unit = {
    val config: Config           = FoxConfig.loadWithEnv()
    implicit val db: DatabaseDef = Database.forConfig("db", config)
    implicit val ac: AC          = ActivityContext(userId = 1, userType = "admin", transactionId = "seeds")

    cfg.mode match {
      case Seed ⇒
        if (cfg.migrateDb) {
          Console.err.println("Cleaning DB and running migrations")
          flyWayMigrate(config)
        }

        if (cfg.seedBase) createBaseSeeds
        if (cfg.seedAdmins) createAdminsSeeds
        if (cfg.seedRandom > 0)
          createRandomSeeds(cfg.seedRandom, cfg.customersScaleMultiplier)
        if (cfg.seedStage) {
          val adminId = mustGetFirstAdmin.id
          createStageSeeds(adminId)
        }
        if (cfg.seedDemo > 0) {
          val adminId = mustGetFirstAdmin.id
          createStageSeeds(adminId)
          createRandomSeeds(cfg.seedDemo, cfg.customersScaleMultiplier)
        }
      case CreateAdmin ⇒
        createAdminManually(name = cfg.adminName,
                            email = cfg.adminEmail,
                            org = cfg.adminOrg,
                            roles = cfg.adminRoles)
      case _ ⇒
        System.err.println(usage)
    }

    db.close()
  }

  def createBaseSeeds(implicit db: DB): Int = {
    Console.err.println("Inserting Base Seeds")
    val result: Failures Xor Int = Await.result(createBase.runTxn(), 4.minutes)
    validateResults("base", result)
  }

  def getFirstAdmin(implicit db: DB): DbResultT[User] =
    Users.take(1).mustFindOneOr(NotFoundFailure404(User, "first"))

  def mustGetFirstAdmin(implicit db: DB): User = {
    val result = Await.result(getFirstAdmin.run(), 1.minute)
    validateResults("get first admin", result)
  }

  def createAdminsSeeds(implicit db: DB, ec: EC, ac: AC): Int = {
    val r = for {
      _      ← * <~ createSingleMerchantSystem
      admins ← * <~ Factories.createStoreAdmins
    } yield admins

    val result: Failures Xor Int = Await.result(r.run(), 4.minutes)
    validateResults("admins", result)
  }

  def createAdminManually(name: String, email: String, org: String, roles: List[String])(
      implicit db: DB,
      ec: EC,
      ac: AC): User = {
    Console.err.println("Create Store Admin seeds")
    val result: Failures Xor User =
      Await.result(Factories.createStoreAdminManual(name, email, org, roles).runTxn(), 1.minute)
    validateResults("admin", result)
  }

  def createStageSeeds(adminId: Int)(implicit db: DB, ac: AC) {
    Console.err.println("Inserting Stage seeds")
    val result: Failures Xor Unit = Await.result(createStage(adminId).runTxn(), 4.minutes)
    validateResults("stage", result)
  }

  def createRandomSeeds(scale: Int, customersScaleMultiplier: Int)(implicit db: DB, ac: AC) {
    Console.err.println("Inserting random seeds")

    val customers            = customersScaleMultiplier * scale
    val batchSize            = Math.min(100, customers)
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

  def createBase(implicit db: DB): DbResultT[Int] =
    for {
      context ← * <~ ObjectContexts.create(SimpleContext.create())
    } yield context.id

  def createAdmins(implicit db: DB, ec: EC, ac: AC): DbResultT[Int] =
    Factories.createStoreAdmins

  val MERCHANT = "merchant"

  def createStage(adminId: Int)(implicit db: DB, ac: AC): DbResultT[Unit] =
    for {
      context ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
      ruContext ← * <~ ObjectContexts.create(
                     SimpleContext.create(name = SimpleContext.ru, lang = "ru"))
      organization ← * <~ Organizations
                      .findByName(MERCHANT)
                      .mustFindOr(OrganizationNotFoundByName(MERCHANT))
      customers   ← * <~ Factories.createCustomers(organization.scopeId)
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

    def reason(storeAdminId: Int) =
      Reason(storeAdminId = storeAdminId, body = "I'm a reason", parentId = None)

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

  def createSingleMerchantSystem(implicit ec: EC) =
    sql""" select bootstrap_single_merchant_system() """.as[Int]

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
