import scala.concurrent.Future
import scala.concurrent.duration._
import akka.actor.{ActorSystem, Props}
import akka.agent.Agent
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer

import cats.data.OptionT
import com.typesafe.config.{Config, ConfigFactory}
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import models._
import org.json4s.jackson
import org.json4s.jackson.Serialization.{write ⇒ json}
import services._
import slick.driver.PostgresDriver.api._
import utils.{RemorseTimer, Tick}

object Main {
  def main(args: Array[String]): Unit = {
    val service = new Service()
    service.bind()
  }
}

class Service(
  systemOverride: Option[ActorSystem] = None,
  dbOverride:     Option[slick.driver.PostgresDriver.backend.DatabaseDef] = None
) {

  import Json4sSupport._
  import utils.JsonFormatters._

  implicit val serialization = jackson.Serialization
  implicit val formats = phoenixFormats

  val config: Config = utils.Config.loadWithEnv()

  implicit val system = systemOverride.getOrElse {
    ActorSystem.create("Orders", config)
  }

  implicit def executionContext = system.dispatcher

  implicit val materializer = ActorMaterializer()

  val logger = Logging(system, getClass)

  implicit val db = dbOverride.getOrElse(Database.forConfig("db", config))

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

  val remorseTimer = system.actorOf(Props(new RemorseTimer()))
  system.scheduler.schedule(Duration.Zero, 1.minute, remorseTimer, Tick)
}
