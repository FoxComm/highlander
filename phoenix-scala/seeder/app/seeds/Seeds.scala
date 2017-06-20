package seeds

import cats.implicits._
import com.github.tminglei.slickpg.LTree
import com.pellucid.sealerate
import com.typesafe.config.Config
import phoenix.failures.UserFailures._
import core.failures.{Failures, FailuresOps, NotFoundFailure404}
import java.time.{Instant, ZoneId}
import org.apache.avro.generic.GenericData
import org.apache.kafka.clients.producer.MockProducer
import org.postgresql.ds.PGSimpleDataSource
import phoenix.models.Reasons
import phoenix.models.account._
import phoenix.models.activity.{ActivityContext, EnrichedActivityContext}
import phoenix.models.auth.UserToken
import objectframework.models.ObjectContexts
import phoenix.models.product.SimpleContext
import phoenix.services.Authenticator.AuthData
import phoenix.services.account.AccountManager
import phoenix.utils.aliases._
import core.db._
import phoenix.utils.db.flyway.{newFlyway, rootProjectSqlLocation}
import phoenix.utils.seeds.Factories
import phoenix.utils.seeds.generators.SeedsGenerator
import phoenix.utils.{ADT, FoxConfig}
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.PostgresProfile.backend.DatabaseDef

object Seeds {

  sealed trait Command
  case object NoCommand           extends Command
  case object CreateAdmin         extends Command
  case object UpdateObjectSchemas extends Command
  case object Seed                extends Command

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
      schemasToUpdate: Seq[String] = Seq(),
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
            .text("Create predefined shipping rules"),
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
          opt[Int]("seedDemo").action((x, c) ⇒ c.copy(seedDemo = x)).text("Create demo seeds")
        )

      cmd("createAdmin")
        .action((_, c) ⇒ c.copy(mode = CreateAdmin))
        .text("Create Admin. Password prompts via stdin or can be set via admin_password env or prop")
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

      cmd("updateObjectSchemas")
        .action((_, c) ⇒ c.copy(mode = UpdateObjectSchemas))
        .text("Update or create Object Schemas")
        .children(
          arg[String]("schema")
            .optional()
            .unbounded()
            .action((x, c) ⇒ c.copy(schemasToUpdate = c.schemasToUpdate :+ x))
            .text("Schemas to update, if ommited update all schemas")
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
    implicit val ac: AC = EnrichedActivityContext(
      ctx = ActivityContext(userId = 1, userType = "user", scope = LTree("1"), transactionId = "seeds"),
      producer = new MockProducer[GenericData.Record, GenericData.Record](true, null, null)
    )

    cfg.mode match {
      case Seed ⇒
        if (cfg.migrateDb) {
          Console.err.println("Cleaning DB and running migrations")
          flyWayMigrate(config)
        }

        if (cfg.seedBase) step("Insert Base Seeds", createBase)
        if (cfg.seedShippingRules) step("Insert Shipping Seeds", createShipmentRules)
        if (cfg.seedAdmins) {
          createStageAdminsSeeds
          step("Create default dictionary values", createDefaultDictionaries)
        }
        if (cfg.seedRandom > 0)
          createRandomSeeds(cfg.seedRandom, cfg.customersScaleMultiplier)
        if (cfg.seedStage) step("Insert Stage seeds", createStageSeeds)
        if (cfg.seedDemo > 0) {
          step("Insert Stage seeds", createStageSeeds)
          createRandomSeeds(cfg.seedDemo, cfg.customersScaleMultiplier)
        }
      case CreateAdmin ⇒
        step("Create Store Admin seeds",
             Factories.createStoreAdminManual(cfg.adminName,
                                              cfg.adminEmail,
                                              cfg.adminOrg,
                                              cfg.adminRoles.split(",").toList))
      case UpdateObjectSchemas ⇒
        // if no schema list is empty upgrade all schemas
        // do this logic to convert arg to Option
        val argSchemasToUpgrade =
          if (cfg.schemasToUpdate.isEmpty)
            None
          else
            Some(cfg.schemasToUpdate)

        step("Upgrade ObjectSchemas", Factories.upgradeObjectSchemas(argSchemasToUpgrade))
      case _ ⇒
        System.err.println(usage)
    }

    db.close()
  }

  def createStageAdminsSeeds(implicit db: DB, ec: EC, ac: AC): Int = {
    val r = for {
      _      ← * <~ Factories.createSingleMerchantSystem
      _      ← * <~ Factories.createSecondStageMerchant
      admins ← * <~ Factories.createStoreAdmins
    } yield admins

    step("Create stage admins seeds", r)
  }

  private[this] def step[T](name: String,
                            f: DbResultT[T],
                            waitFor: FiniteDuration = 4.minute)(implicit db: DB, ec: EC, ac: AC): T = {
    Console.out.println(name)
    // TODO: Should we really be discarding all warnings here (and git-grep 'runEmptyA')? Rethink! @michalrus
    val result: Either[Failures, T] = Await.result(f.runTxn().runEmptyA.value, waitFor)
    validateResults(name, result)
  }

  val MERCHANT       = "merchant"
  val MERCHANT_EMAIL = "hackerman@yahoo.com"

  def getMerchant(implicit db: DB, ac: AC): DbResultT[(Organization, User, Account, Account.ClaimSet)] =
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
    val customers            = customersScaleMultiplier * scale
    val batchSize            = Math.min(100, customers)
    val appeasementsPerBatch = 8
    val batches              = customers / batchSize

    Console.err.println(s"Generating $customers customers in $batches batches")

    // Have to generate data in batches because of DBIO.seq stack overflow bug.
    // https://github.com/slick/slick/issues/1186
    (1 to batches).foreach { b ⇒
      Console.err.println(s"Generating random batch $b of $batches total")
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

      step(s"Random batch $b", r)
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

  def createDefaultDictionaries(implicit db: DB, ac: AC, ec: EC): DbResultT[Unit] =
    for {
      admin ← * <~ Users.take(1).mustFindOneOr(NotFoundFailure404(User, "first"))
      _     ← * <~ Reasons.createAll(Factories.reasons.map(_.copy(storeAdminId = admin.id)))
      _     ← * <~ Factories.createReturnReasons
    } yield {}

  def createStageSeeds(implicit db: DB, ac: AC): DbResultT[Unit] =
    for {
      r ← * <~ getMerchant
      (organization, merchant, account, claims) = r
      _ ← * <~ {
           implicit val au = AuthData[User](token = UserToken.fromUserAccount(merchant, account, claims),
                                            model = merchant,
                                            account = account)

           for {
             admin   ← * <~ Users.take(1).mustFindOneOr(NotFoundFailure404(User, "first"))
             context ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
             ruContext ← * <~ ObjectContexts.create(
                          SimpleContext.create(name = SimpleContext.ru, lang = "ru"))
             customers  ← * <~ Factories.createCustomers(organization.scopeId)
             _          ← * <~ Factories.createAddresses(customers)
             _          ← * <~ Factories.createCreditCards(customers)
             products   ← * <~ Factories.createProducts
             ruProducts ← * <~ Factories.createRuProducts(products)
             _          ← * <~ Factories.createGiftCards
             _          ← * <~ Factories.createStoreCredits(admin.id, customers._1, customers._3)
             _          ← * <~ Factories.createShipmentRules

             // Promotions
             search     ← * <~ Factories.createSharedSearches(admin.id)
             discounts  ← * <~ Factories.createDiscounts(search)
             promotions ← * <~ Factories.createCouponPromotions(discounts)
             coupons    ← * <~ Factories.createCoupons(promotions)
           } yield {}
         }
    } yield {}

  private def flyWayMigrate(config: Config): Unit = {
    val flyway = newFlyway(jdbcDataSourceFromConfig("db", config), rootProjectSqlLocation)

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

  private def validateResults[R](seed: String, result: Either[Failures, R])(implicit db: DB): R =
    result.fold(
      failures ⇒ {
        Console.err.println(s"'$seed' has failed!")
        failures.flatten.foreach(Console.err.println)
        db.close()
        sys.exit(1)
      },
      v ⇒ { Console.err.println(s"Successfully completed '$seed'!"); v }
    )
}
