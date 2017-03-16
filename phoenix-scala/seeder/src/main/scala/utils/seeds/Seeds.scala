package utils.seeds

import com.github.tminglei.slickpg.LTree

import cats._
import cats.data._
import cats.implicits._
import com.pellucid.sealerate
import com.typesafe.config.Config
import failures.UserFailures._
import failures.{Failures, FailuresOps, NotFoundFailure404}
import java.time.{Instant, ZoneId}
import models.Reasons
import models.account._
import models.activity.ActivityContext
import models.auth.UserToken
import models.objects.ObjectContexts
import models.product.SimpleContext
import org.postgresql.ds.PGSimpleDataSource
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import services.Authenticator.AuthData
import services.account.AccountManager
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.DatabaseDef
import utils.aliases._
import utils.db._
import utils.db.flyway.newFlyway
import utils.seeds.generators.SeedsGenerator
import utils.{ADT, FoxConfig}

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
      seedShippingRules: Boolean = false,
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
      adminRoles: String = ""
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
            opt[Unit]("seedShippingRules")
              .action((_, c) ⇒ c.copy(seedShippingRules = true))
              .text("Create predefined admins"),
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
              .text("Admin email"),
            opt[String]("org").required().action((x, c) ⇒ c.copy(adminOrg = x)).text("Admin Org"),
            opt[String]("roles")
              .required()
              .action((x, c) ⇒ c.copy(adminRoles = x))
              .text("Admin Roles")
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
    val config: Config           = FoxConfig.unsafe
    implicit val db: DatabaseDef = Database.forConfig("db", config)
    implicit val ac: AC = ActivityContext
      .build(userId = 1, userType = "user", scope = LTree(""), transactionId = "seeds")

    cfg.mode match {
      case Seed ⇒
        if (cfg.migrateDb) {
          Console.err.println("Cleaning DB and running migrations")
          flyWayMigrate(config)
        }

        if (cfg.seedBase) createBaseSeeds
        if (cfg.seedShippingRules) createShippingRulesSeeds
        if (cfg.seedAdmins) createStageAdminsSeeds
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
                            roles = cfg.adminRoles.split(",").toList)
      case _ ⇒
        System.err.println(usage)
    }

    db.close()
  }

  def createBaseSeeds(implicit db: DB): Int = {
    Console.err.println("Inserting Base Seeds")
    // TODO: Should we really be discarding all warnings here (and git-grep 'runEmptyA')? Rethink! @michalrus
    val result: Failures Xor Int = Await.result(createBase.runTxn().runEmptyA.value, 4.minutes)
    validateResults("base", result)
  }

  def createShippingRulesSeeds(implicit db: DB): Int = {
    Console.err.println("Inserting Shipping Seeds")
    val result: Failures Xor Int =
      Await.result(createShipmentRules.runTxn().runEmptyA.value, 4.minutes)
    validateResults("shipping", result)
  }

  def getFirstAdmin(implicit db: DB): DbResultT[User] =
    Users.take(1).mustFindOneOr(NotFoundFailure404(User, "first"))

  def mustGetFirstAdmin(implicit db: DB): User = {
    val result = Await.result(getFirstAdmin.runDBIO().runEmptyA.value, 1.minute)
    validateResults("get first admin", result)
  }

  def createStageAdminsSeeds(implicit db: DB, ec: EC, ac: AC): Int = {
    val r = for {
      _      ← * <~ Factories.createSingleMerchantSystem
      _      ← * <~ Factories.createSecondStageMerchant
      admins ← * <~ Factories.createStoreAdmins
    } yield admins

    val result: Failures Xor Int = Await.result(r.runDBIO().runEmptyA.value, 4.minutes)
    validateResults("admins", result)
  }

  def createAdminManually(name: String, email: String, org: String, roles: List[String])(
      implicit db: DB,
      ec: EC,
      ac: AC): User = {
    Console.err.println("Create Store Admin seeds")
    val result: Failures Xor User = Await.result(
        Factories.createStoreAdminManual(name, email, org, roles).runTxn().runEmptyA.value,
        1.minute)
    validateResults("admin", result)
  }

  def createStageSeeds(adminId: Int)(implicit db: DB, ac: AC) {
    Console.err.println("Inserting Stage seeds")
    val result: Failures Xor Unit =
      Await.result(createStage(adminId).runTxn().runEmptyA.value, 4.minutes)
    validateResults("stage", result)
  }

  val MERCHANT       = "merchant"
  val MERCHANT_EMAIL = "hackerman@yahoo.com"

  def getMerchant(implicit db: DB,
                  ac: AC): DbResultT[(Organization, User, Account, Account.ClaimSet)] =
    for {
      organization ← * <~ Organizations
                      .findByName(MERCHANT)
                      .mustFindOr(OrganizationNotFoundByName(MERCHANT))
      merchant ← * <~ Users
                  .findByEmail(MERCHANT_EMAIL)
                  .mustFindOneOr(NotFoundFailure404(User, MERCHANT_EMAIL))
      account ← * <~ Accounts.mustFindById404(merchant.accountId)
      claims  ← * <~ AccountManager.getClaims(merchant.accountId, organization.scopeId)
    } yield (organization, merchant, account, claims)

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
      val r = for {
        r ← * <~ getMerchant
        (organization, merchant, account, claims) = r
        _ ← * <~ {
             implicit val au =
               AuthData[User](token = UserToken.fromUserAccount(merchant, account, claims),
                              model = merchant,
                              account = account)

             for {
               _ ← * <~ SeedsGenerator.insertRandomizedSeeds(batchSize, appeasementsPerBatch)
             } yield {}
           }
      } yield {}

      val result = Await.result(r.runTxn().runEmptyA.value, (120 * scale).second)
      validateResults(s"random batch $b", result)
    }
  }

  val today = Instant.now().atZone(ZoneId.of("UTC"))

  def createBase(implicit db: DB): DbResultT[Int] =
    for {
      context ← * <~ ObjectContexts.create(SimpleContext.create())
      _       ← * <~ Factories.createObjectSchemas
    } yield context.id

  def createShipmentRules(implicit db: DB): DbResultT[Int] =
    for {
      _ ← * <~ Factories.createShipmentRules
    } yield 0

  def createAdmins(implicit db: DB, ec: EC, ac: AC): DbResultT[Int] =
    Factories.createStoreAdmins

  def createStage(adminId: Int)(implicit db: DB, ac: AC): DbResultT[Unit] =
    for {
      r ← * <~ getMerchant
      (organization, merchant, account, claims) = r
      _ ← * <~ {
           implicit val au = AuthData[User](token =
                                              UserToken.fromUserAccount(merchant, account, claims),
                                            model = merchant,
                                            account = account)

           for {
             context ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
             ruContext ← * <~ ObjectContexts.create(
                            SimpleContext.create(name = SimpleContext.ru, lang = "ru"))
             customers  ← * <~ Factories.createCustomers(organization.scopeId)
             _          ← * <~ Factories.createAddresses(customers)
             _          ← * <~ Factories.createCreditCards(customers)
             products   ← * <~ Factories.createProducts
             ruProducts ← * <~ Factories.createRuProducts(products)
             _          ← * <~ Reasons.createAll(Factories.reasons.map(_.copy(storeAdminId = adminId)))
             _          ← * <~ Factories.createGiftCards
             _          ← * <~ Factories.createStoreCredits(adminId, customers._1, customers._3)
             _          ← * <~ Factories.createShipmentRules
             // Promotions
             search     ← * <~ Factories.createSharedSearches(adminId)
             discounts  ← * <~ Factories.createDiscounts(search)
             promotions ← * <~ Factories.createCouponPromotions(discounts)
             coupons    ← * <~ Factories.createCoupons(promotions)
           } yield {}
         }
    } yield {}

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
