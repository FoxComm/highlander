package server

import scala.collection.immutable
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}
import akka.actor.{ActorSystem, Cancellable, Props}
import akka.agent.Agent
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route}
import akka.stream.ActorMaterializer

import com.typesafe.config.Config
import models.StoreAdmin
import models.customer.Customer
import org.json4s.jackson.Serialization
import org.json4s.{Formats, jackson}
import routes.admin.DevRoutes
import services.Authenticator
import services.Authenticator.{AsyncAuthenticator, requireAuth}
import services.actors._
import slick.driver.PostgresDriver.api._
import utils.Config.{Development, Staging}
import utils.{Apis, CustomHandlers, WiredStripeApi}


object Main extends App {
  implicit val env = utils.Config.environment
  val service = new Service()
  service.performSelfCheck()
  service.bind()
  service.setupRemorseTimers
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
      logRequestResult("auth-routes")(routes.AuthRoutes.routes) ~
      logRequestResult("public-routes")(routes.Public.routes) ~
      logRequestResult("notification-routes")(routes.NotificationRoutes.routes) ~
      logRequestResult("customer-routes")(routes.Customer.routes) ~
      requireAuth(storeAdminAuth) { implicit admin ⇒
        logRequestResult("admin-routes")(routes.admin.AdminRoutes.routes) ~
        logRequestResult("admin-notification-routes")(routes.NotificationRoutes.adminRoutes) ~
        logRequestResult("admin-assignments-routes")(routes.admin.AssignmentsRoutes.routes) ~
        logRequestResult("admin-order-routes")(routes.admin.OrderRoutes.routes) ~
        logRequestResult("admin-customer-routes")(routes.admin.CustomerRoutes.routes) ~
        logRequestResult("admin-customer-groups-routes")(routes.admin.CustomerGroupsRoutes.routes) ~
        logRequestResult("admin-giftcard-routes")(routes.admin.GiftCardRoutes.routes) ~
        logRequestResult("admin-rma-routes")(routes.admin.RmaRoutes.routes) ~
        logRequestResult("admin-activity-routes")(routes.admin.Activity.routes) ~
        logRequestResult("admin-inventory-routes")(routes.admin.InventoryRoutes.routes) ~
        logRequestResult("admin-product-routes")(routes.admin.ProductRoutes.routes) ~
        logRequestResult("admin-sku-routes")(routes.admin.SkuRoutes.routes) ~
        logRequestResult("admin-discount-routes")(routes.admin.DiscountRoutes.routes) ~
        logRequestResult("admin-promotion-routes")(routes.admin.PromotionRoutes.routes) ~
        logRequestResult("admin-coupon-routes")(routes.admin.CouponRoutes.routes)
      }
    }
  }

  val devRoutes = {
    pathPrefix("v1") {
      requireAuth(storeAdminAuth) { implicit admin ⇒
        logRequestResult("dev-routes")(routes.admin.DevRoutes.routes)
      }
    }
  }

  val allRoutes = utils.Config.environment match {
    case Development | Staging ⇒ addRoutes.foldLeft(defaultRoutes ~ devRoutes)(_ ~ _)
    case _                     ⇒ addRoutes.foldLeft(defaultRoutes)(_ ~ _)
  }

  implicit def rejectionHandler: RejectionHandler = CustomHandlers.jsonRejectionHandler

  implicit def exceptionHandler: ExceptionHandler = CustomHandlers.jsonExceptionHandler

  private final val serverBinding = Agent[Option[ServerBinding]](None)

  def bind(config: Config = config): Future[ServerBinding] =
    Http().bindAndHandle(allRoutes, config.getString("http.interface"), config.getInt("http.port")).flatMap {
      binding ⇒ serverBinding.alter(Some(binding)).map(_ ⇒ binding)
    }

  def close(): Future[Unit] =
    serverBinding.future.flatMap {
      case Some(b) ⇒ b.unbind()
      case None    ⇒ Future.successful(())
    }

  def setupRemorseTimers: Cancellable = {
    val remorseTimer = system.actorOf(Props(new RemorseTimer()), "remorse-timer")
    val remorseTimerBuddy = system.actorOf(Props(new RemorseTimerMate()), "remorse-timer-mate")
    system.scheduler.schedule(Duration.Zero, 1.minute, remorseTimer, Tick)(executionContext, remorseTimerBuddy)
  }

  def performSelfCheck(): Unit = {
    if (config.getString("auth.method") == "jwt") {
      import models.auth.Keys
      assert(Keys.loadPrivateKey.isSuccess, "Can't load private key")
      assert(Keys.loadPublicKey.isSuccess, "Can't load public key")
    }
  }

}
