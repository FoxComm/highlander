package server

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.concurrent.duration._
import akka.actor.{Cancellable, ActorSystem, Props}
import akka.agent.Agent
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer

import com.typesafe.config.Config
import models._
import org.json4s.{Formats, jackson}
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.{write ⇒ json}
import services._
import slick.driver.PostgresDriver
import slick.driver.PostgresDriver.api._
import utils.{RemorseTimer, RemorseTimerMate, Tick}

object Main {
  def main(args: Array[String]): Unit = {
    val service = new Service()
    service.bind()
    service.setupRemorseTimers
  }
}

class Service(
  systemOverride: Option[ActorSystem] = None,
  dbOverride:     Option[Database]    = None
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

  implicit val db: Database = dbOverride.getOrElse(Database.forConfig("db", config))

  implicit def storeAdminAuth: AsyncAuthenticator[StoreAdmin] = Authenticator.storeAdmin

  implicit def customerAuth: AsyncAuthenticator[Customer] = Authenticator.customer

  val allRoutes = {
    pathPrefix("v1") {
      logRequestResult("admin-routes")(routes.Admin.routes) ~
      logRequestResult("customer-routes")(routes.Customer.routes) ~
      logRequestResult("public-routes")(routes.Public.routes)
    }
  }

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
