package server

import scala.collection.immutable
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import akka.actor.{ActorSystem, Props}
import akka.agent.Agent
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RouteResult._
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import com.stripe.Stripe
import com.typesafe.scalalogging.LazyLogging
import models.account.AccountAccessMethod
import org.json4s._
import org.json4s.jackson._
import services.account.AccountCreateContext
import services.Authenticator
import services.Authenticator.UserAuthenticator
import services.Authenticator.requireAdminAuth
import services.actors._
import slick.driver.PostgresDriver.api._
import utils.apis._
import utils.http.CustomHandlers
import utils.http.HttpLogger.logFailedRequests
import utils.{ElasticsearchApi, Environment, FoxConfig}

object Main extends App with LazyLogging {
  logger.info("Starting phoenix server")

  val service = new Service()

  try {
    service.performSelfCheck()
    service.bind()
    service.setupRemorseTimers()

    logger.info("Startup process complete")
  } catch {
    case e: Throwable ⇒
      logger.error(s"${e.getMessage}\nExiting now!")
      Thread.sleep(1000)
      System.exit(1)
  }
}

class Service(
    systemOverride: Option[ActorSystem] = None,
    dbOverride: Option[Database] = None,
    apisOverride: Option[Apis] = None,
    esOverride: Option[ElasticsearchApi] = None,
    addRoutes: immutable.Seq[Route] = immutable.Seq.empty)(implicit val env: Environment) {

  import FoxConfig.config
  import utils.JsonFormatters._

  implicit val serialization: Serialization.type = jackson.Serialization
  implicit val formats: Formats                  = phoenixFormats

  implicit val system: ActorSystem = systemOverride.getOrElse {
    ActorSystem.create("Orders", FoxConfig.unsafe)
  }

  val threadPool                = java.util.concurrent.Executors.newCachedThreadPool()
  implicit val executionContext = ExecutionContext.fromExecutor(threadPool)

  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val logger = Logging(system, getClass)

  implicit val db: Database  = dbOverride.getOrElse(Database.forConfig("db", FoxConfig.unsafe))
  lazy val defaultApis: Apis = Apis(setupStripe(), new AmazonS3, setupMiddlewarehouse())
  implicit val apis: Apis    = apisOverride.getOrElse(defaultApis: Apis)
  implicit val es: ElasticsearchApi =
    esOverride.getOrElse(ElasticsearchApi.fromConfig(FoxConfig.config))

  val roleName = config.users.customer.role
  val orgName  = config.users.customer.org
  val scopeId  = config.users.customer.scopeId

  val customerCreateContext                = AccountCreateContext(List(roleName), orgName, scopeId)
  implicit val userAuth: UserAuthenticator = Authenticator.forUser(customerCreateContext)

  val defaultRoutes = {
    pathPrefix("v1") {
      routes.AuthRoutes.routes ~
      routes.Public.routes(customerCreateContext) ~
      routes.Customer.routes ~
      requireAdminAuth(userAuth) { implicit auth ⇒
        routes.admin.AdminRoutes.routes ~
        routes.admin.NotificationRoutes.routes ~
        routes.admin.AssignmentsRoutes.routes ~
        routes.admin.OrderRoutes.routes ~
        routes.admin.CartRoutes.routes ~
        routes.admin.CustomerRoutes.routes ~
        routes.admin.CustomerGroupsRoutes.routes ~
        routes.admin.GiftCardRoutes.routes ~
        routes.admin.ReturnRoutes.routes ~
        routes.admin.Activity.routes ~
        routes.admin.ProductRoutes.routes ~
        routes.admin.SkuRoutes.routes ~
        routes.admin.VariantRoutes.routes ~
        routes.admin.DiscountRoutes.routes ~
        routes.admin.PromotionRoutes.routes ~
        routes.admin.ImageRoutes.routes ~
        routes.admin.CouponRoutes.routes ~
        routes.admin.CategoryRoutes.routes ~
        routes.admin.GenericTreeRoutes.routes ~
        routes.admin.StoreAdminRoutes.routes ~
        routes.admin.ObjectRoutes.routes ~
        routes.admin.PluginRoutes.routes ~
        routes.admin.TaxonomyRoutes.routes ~
        routes.service.PaymentRoutes.routes ~ //Migrate this to auth with service tokens once we have them
        routes.service.MigrationRoutes.routes(customerCreateContext)
      }
    }
  }

  lazy val devRoutes = {
    pathPrefix("v1") {
      requireAdminAuth(userAuth) { implicit auth ⇒
        routes.admin.DevRoutes.routes
      }
    }
  }

  val allRoutes = {
    val routes = if (env.isDev) {
      logger.info("Activating dev routes")
      addRoutes.foldLeft(defaultRoutes ~ devRoutes)(_ ~ _)
    } else
      addRoutes.foldLeft(defaultRoutes)(_ ~ _)
    logFailedRequests(routes, logger)
  }

  implicit def rejectionHandler: RejectionHandler = CustomHandlers.jsonRejectionHandler

  implicit def exceptionHandler: ExceptionHandler = CustomHandlers.jsonExceptionHandler

  private final val serverBinding = Agent[Option[ServerBinding]](None)

  def bind(config: FoxConfig = config): Future[ServerBinding] = {
    val host = config.http.interface
    val port = config.http.port
    val bind = Http().bindAndHandle(allRoutes, host, port).flatMap { binding ⇒
      serverBinding.alter(Some(binding)).map(_ ⇒ binding)
    }
    logger.info(s"Bound to $host:$port")
    bind
  }

  def close(): Future[Unit] =
    serverBinding.future.flatMap {
      case Some(b) ⇒ b.unbind()
      case None    ⇒ Future.successful(())
    }

  def setupRemorseTimers(): Unit = {
    logger.info("Scheduling remorse timer")
    val remorseTimer      = system.actorOf(Props(new RemorseTimer()), "remorse-timer")
    val remorseTimerBuddy = system.actorOf(Props(new RemorseTimerMate()), "remorse-timer-mate")
    system.scheduler
      .schedule(Duration.Zero, 1.minute, remorseTimer, Tick)(executionContext, remorseTimerBuddy)
  }

  def performSelfCheck(): Unit = {
    logger.info("Performing self check")
    if (config.auth.method == FoxConfig.AuthMethod.Jwt) {
      import models.auth.Keys
      assert(Keys.loadPrivateKey.isSuccess, "Can't load private key")
      assert(Keys.loadPublicKey.isSuccess, "Can't load public key")
    }
    logger.info(s"Using password hash algorithm: ${AccountAccessMethod.passwordsHashAlgorithm}")
    logger.info("Self check complete")
  }

  def setupStripe(): FoxStripe = {
    logger.info("Loading Stripe API key")
    Stripe.apiKey = config.apis.stripe.key
    logger.info("Successfully set Stripe key")
    new FoxStripe(new StripeWrapper())
  }

  def setupMiddlewarehouse(): Middlewarehouse =
    new Middlewarehouse(config.apis.middlewarehouse.url)
}
