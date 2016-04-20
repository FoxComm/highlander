package server

import scala.collection.immutable
import scala.concurrent.{ExecutionContextExecutor, Future}
import akka.actor.{ActorSystem, Props}
import akka.event.Logging
import scala.concurrent.duration._
import akka.agent.Agent
import akka.event.Logging.ErrorLevel
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RouteResult._
import akka.http.scaladsl.server.directives.LoggingMagnet
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import models.StoreAdmin
import models.customer.Customer
import org.json4s.jackson.Serialization
import org.json4s.{Formats, jackson}
import services.Authenticator
import services.Authenticator.{AsyncAuthenticator, requireAuth}
import services.actors._
import slick.driver.PostgresDriver.api._
import utils.Config.{Development, Staging}
import utils.{Apis, CustomHandlers, WiredStripeApi}

object Main extends App with LazyLogging {
  logger.info("Starting phoenix server")
  implicit val env = utils.Config.environment
  val service = new Service()
  service.performSelfCheck()
  service.bind()
  service.setupRemorseTimers()
  logger.info("Startup process complete")
}

class Service(
  systemOverride: Option[ActorSystem]  = None,
  dbOverride:     Option[Database]     = None,
  apisOverride:   Option[Apis]         = None,
  addRoutes:      immutable.Seq[Route] = immutable.Seq.empty)(implicit val env: utils.Config.Environment) {

  import utils.JsonFormatters._

  implicit val serialization: Serialization.type = jackson.Serialization
  implicit val formats: Formats = phoenixFormats

  val config: Config = utils.Config.loadWithEnv()

  implicit val system: ActorSystem = systemOverride.getOrElse {
    ActorSystem.create("Orders", config)
  }

  implicit def executionContext: ExecutionContextExecutor = system.dispatcher

  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val logger = Logging(system, getClass)

  implicit val db:   Database = dbOverride.getOrElse(Database.forConfig("db", config))
  implicit val apis: Apis     = apisOverride.getOrElse(Apis(new WiredStripeApi))

  val storeAdminAuth: AsyncAuthenticator[StoreAdmin] = Authenticator.forAdminFromConfig
  implicit val customerAuth: AsyncAuthenticator[Customer] = Authenticator.forCustomerFromConfig

  val defaultRoutes = {
    pathPrefix("v1") {
      logFailedRequests("auth-routes", ErrorLevel)(routes.AuthRoutes.routes) ~
      logFailedRequests("public-routes", ErrorLevel)(routes.Public.routes) ~
      logFailedRequests("customer-routes", ErrorLevel)(routes.Customer.routes) ~
      requireAuth(storeAdminAuth) { implicit admin ⇒
        logFailedRequests("admin-routes", ErrorLevel)(routes.admin.AdminRoutes.routes) ~
        logFailedRequests("admin-notification-routes", ErrorLevel)(routes.admin.NotificationRoutes.routes) ~
        logFailedRequests("admin-assignments-routes", ErrorLevel)(routes.admin.AssignmentsRoutes.routes) ~
        logFailedRequests("admin-order-routes", ErrorLevel)(routes.admin.OrderRoutes.routes) ~
        logFailedRequests("admin-customer-routes", ErrorLevel)(routes.admin.CustomerRoutes.routes) ~
        logFailedRequests("admin-customer-groups-routes", ErrorLevel)(routes.admin.CustomerGroupsRoutes.routes) ~
        logFailedRequests("admin-giftcard-routes", ErrorLevel)(routes.admin.GiftCardRoutes.routes) ~
        logFailedRequests("admin-rma-routes", ErrorLevel)(routes.admin.RmaRoutes.routes) ~
        logFailedRequests("admin-activity-routes", ErrorLevel)(routes.admin.Activity.routes) ~
        logFailedRequests("admin-inventory-routes", ErrorLevel)(routes.admin.InventoryRoutes.routes) ~
        logFailedRequests("admin-product-routes", ErrorLevel)(routes.admin.ProductRoutes.routes) ~
        logFailedRequests("admin-sku-routes", ErrorLevel)(routes.admin.SkuRoutes.routes) ~
        logFailedRequests("admin-discount-routes", ErrorLevel)(routes.admin.DiscountRoutes.routes) ~
        logFailedRequests("admin-promotion-routes", ErrorLevel)(routes.admin.PromotionRoutes.routes) ~
        logFailedRequests("admin-coupon-routes", ErrorLevel)(routes.admin.CouponRoutes.routes)
      }
    }
  }

  val devRoutes = {
    pathPrefix("v1") {
      requireAuth(storeAdminAuth) { implicit admin ⇒
        logFailedRequests("dev-routes", ErrorLevel)(routes.admin.DevRoutes.routes)
      }
    }
  }

  val allRoutes = utils.Config.environment match {
    case Development | Staging ⇒
      logger.info("Activating dev routes")
      addRoutes.foldLeft(defaultRoutes ~ devRoutes)(_ ~ _)
    case _ ⇒
      addRoutes.foldLeft(defaultRoutes)(_ ~ _)
  }

  private def logFailedRequests(magnet: LoggingMagnet[HttpRequest ⇒ RouteResult ⇒ Unit]): Directive0 = {
    extractRequestContext.flatMap { ctx ⇒
      lazy val logResult = magnet.f(logger)(ctx.request)

      mapRouteResult { result ⇒
        result match {
          case Complete(response) ⇒ response.status match {
            case StatusCodes.OK ⇒ // NOOP
            case _ ⇒ logResult(result)
          }
          case Rejected(rejections) ⇒ logResult(result)
        }
        result
      }
    }
  }

  implicit def rejectionHandler: RejectionHandler = CustomHandlers.jsonRejectionHandler

  implicit def exceptionHandler: ExceptionHandler = CustomHandlers.jsonExceptionHandler

  private final val serverBinding = Agent[Option[ServerBinding]](None)

  def bind(config: Config = config): Future[ServerBinding] = {
    val host = config.getString("http.interface")
    val port = config.getInt("http.port")
    val bind = Http().bindAndHandle(allRoutes, host, port).flatMap {
      binding ⇒ serverBinding.alter(Some(binding)).map(_ ⇒ binding)
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
    val remorseTimer = system.actorOf(Props(new RemorseTimer()), "remorse-timer")
    val remorseTimerBuddy = system.actorOf(Props(new RemorseTimerMate()), "remorse-timer-mate")
    system.scheduler.schedule(Duration.Zero, 1.minute, remorseTimer, Tick)(executionContext, remorseTimerBuddy)
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
