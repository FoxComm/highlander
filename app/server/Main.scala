package server

import akka.actor.{ActorSystem, Cancellable, Props}
import akka.agent.Agent
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route}
import akka.stream.ActorMaterializer
import com.typesafe.config.Config
import models.{Customer, StoreAdmin}
import org.json4s.jackson.Serialization
import org.json4s.{Formats, jackson}
import services.Authenticator
import slick.driver.PostgresDriver.api._
import utils.{Apis, CustomHandlers, RemorseTimer, RemorseTimerMate, Tick, WiredStripeApi}

import scala.collection.immutable
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}

object Main extends App {
  val service = new Service()
  service.bind()
  service.setupRemorseTimers
}

class Service(
  systemOverride: Option[ActorSystem]  = None,
  dbOverride:     Option[Database]     = None,
  apisOverride:   Option[Apis]         = None,
  addRoutes:      immutable.Seq[Route] = immutable.Seq.empty
) {

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

  implicit def storeAdminAuth: AsyncAuthenticator[StoreAdmin] = Authenticator.storeAdmin

  implicit def customerAuth: AsyncAuthenticator[Customer] = Authenticator.customer

  val defaultRoutes = {
    pathPrefix("v1") {
      logRequestResult("admin-routes")(routes.admin.Admin.routes) ~
      logRequestResult("admin-order-routes")(routes.admin.OrderRoutes.routes) ~
      logRequestResult("admin-customer-routes")(routes.admin.CustomerRoutes.routes) ~
      logRequestResult("admin-giftcard-routes")(routes.admin.GiftCardRoutes.routes) ~
      logRequestResult("admin-rma-routes")(routes.admin.RmaRoutes.routes) ~
      logRequestResult("admin-activity-routes")(routes.admin.Activity.routes) ~
      logRequestResult("customer-routes")(routes.Customer.routes) ~
      logRequestResult("public-routes")(routes.Public.routes)
    }
  }

  val allRoutes = addRoutes.foldLeft(defaultRoutes)(_ ~ _)

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
}
