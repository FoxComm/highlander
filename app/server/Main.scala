package server

import cats.implicits._
import scala.collection.immutable
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import akka.actor.{ActorSystem, Props}
import akka.event.{Logging, LoggingAdapter}
import scala.concurrent.duration._
import akka.agent.Agent
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RouteResult._
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import models.StoreAdmin
import models.customer.Customer
import org.json4s.JsonAST.JString
import org.json4s.jackson._
import org.json4s._
import services.Authenticator
import services.Authenticator.{AsyncAuthenticator, requireAuth}
import services.actors._
import slick.driver.PostgresDriver.api._
import utils.FoxConfig.{Development, Staging}
import utils.http.CustomHandlers
import utils.http.HttpLogger.logFailedRequests
import utils.{ElasticsearchApi, FoxConfig}
import utils.apis._

object Main extends App with LazyLogging {
  logger.info("Starting phoenix server")
  implicit val env = FoxConfig.environment
  val service      = new Service()
  service.performSelfCheck()
  service.bind()
  service.setupRemorseTimers()

  logger.info("Startup process complete")
}

class Service(systemOverride: Option[ActorSystem] = None,
              dbOverride: Option[Database] = None,
              apisOverride: Option[Apis] = None,
              esOverride: Option[ElasticsearchApi] = None,
              addRoutes: immutable.Seq[Route] = immutable.Seq.empty)(
    implicit val env: FoxConfig.Environment) {

  import utils.JsonFormatters._

  implicit val serialization: Serialization.type = jackson.Serialization
  implicit val formats: Formats                  = phoenixFormats

  val config: Config = FoxConfig.loadWithEnv()

  implicit val system: ActorSystem = systemOverride.getOrElse {
    ActorSystem.create("Orders", config)
  }

  implicit def executionContext: ExecutionContextExecutor = system.dispatcher

  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val logger = Logging(system, getClass)

  implicit val db: Database         = dbOverride.getOrElse(Database.forConfig("db", config))
  implicit val apis: Apis           = apisOverride.getOrElse(Apis(new WiredStripeApi, new AmazonS3))
  implicit val es: ElasticsearchApi = esOverride.getOrElse(ElasticsearchApi.fromConfig(config))

  val storeAdminAuth: AsyncAuthenticator[StoreAdmin]      = Authenticator.forAdminFromConfig
  implicit val customerAuth: AsyncAuthenticator[Customer] = Authenticator.forCustomerFromConfig

  val defaultRoutes = {
    pathPrefix("v1") {
      routes.AuthRoutes.routes ~
      routes.Public.routes ~
      routes.Customer.routes ~
      requireAuth(storeAdminAuth) { implicit admin ⇒
        routes.admin.AdminRoutes.routes ~
        routes.admin.NotificationRoutes.routes ~
        routes.admin.AssignmentsRoutes.routes ~
        routes.admin.OrderRoutes.routes ~
        routes.admin.CustomerRoutes.routes ~
        routes.admin.CustomerGroupsRoutes.routes ~
        routes.admin.GiftCardRoutes.routes ~
        routes.admin.ReturnRoutes.routes ~
        routes.admin.Activity.routes ~
        routes.admin.InventoryRoutes.routes ~
        routes.admin.ProductRoutes.routes ~
        routes.admin.SkuRoutes.routes ~
        routes.admin.VariantRoutes.routes ~
        routes.admin.DiscountRoutes.routes ~
        routes.admin.PromotionRoutes.routes ~
        routes.admin.ImageRoutes.routes ~
        routes.admin.CouponRoutes.routes ~
        routes.admin.CategoryRoutes.routes ~
        routes.admin.GenericTreeRoutes.routes
      }
    }
  }

  val devRoutes = {
    pathPrefix("v1") {
      requireAuth(storeAdminAuth) { implicit admin ⇒
        routes.admin.DevRoutes.routes
      }
    }
  }

  val allRoutes = {
    val routes = FoxConfig.environment match {
      case Development | Staging ⇒
        logger.info("Activating dev routes")
        addRoutes.foldLeft(defaultRoutes ~ devRoutes)(_ ~ _)
      case _ ⇒
        addRoutes.foldLeft(defaultRoutes)(_ ~ _)
    }
    logFailedRequests(routes, logger)
  }

  implicit def rejectionHandler: RejectionHandler = CustomHandlers.jsonRejectionHandler

  implicit def exceptionHandler: ExceptionHandler = CustomHandlers.jsonExceptionHandler

  private final val serverBinding = Agent[Option[ServerBinding]](None)

  def bind(config: Config = config): Future[ServerBinding] = {
    val host = config.getString("http.interface")
    val port = config.getInt("http.port")
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
    system.scheduler.schedule(Duration.Zero, 1.minute, remorseTimer, Tick)(
        executionContext, remorseTimerBuddy)
  }

  def performSelfCheck(): Unit = {
    logger.info("Performing self check")
    if (config.getString("auth.method") == "jwt") {
      import models.auth.Keys
      assert(Keys.loadPrivateKey.isSuccess, "Can't load private key")
      assert(Keys.loadPublicKey.isSuccess, "Can't load public key")
    }
    logger.info("Self check complete")
  }
}
