import scala.concurrent.Future
import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer

import com.typesafe.config.{Config, ConfigFactory}
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import models._
import org.json4s.jackson
import org.json4s.jackson.Serialization.{write ⇒ json}
import services._
import slick.driver.PostgresDriver.api._

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

  val conf: String =
    """
      |akka {
      |  loglevel = "DEBUG"
      |  loggers = ["akka.event.Logging$DefaultLogger"]
      |  actor.debug.receive = on
      |}
      |
      |http {
      |  interface = "localhost"
      |  port = 9090
      |}
    """.stripMargin

  val config: Config = ConfigFactory.parseString(conf)

  implicit val system = systemOverride.getOrElse {
    ActorSystem.create("Orders", config)
  }

  implicit def executionContext = system.dispatcher

  implicit val materializer = ActorMaterializer()

  val logger = Logging(system, getClass)

  implicit val db = dbOverride.getOrElse(Database.forConfig("db.development"))

  implicit def storeAdminAuth: AsyncAuthenticator[StoreAdmin] = Authenticator.storeAdmin

  implicit def customerAuth: AsyncAuthenticator[Customer] = Authenticator.customer

  val allRoutes = {
    pathPrefix("v1") {
      logRequestResult("admin-routes")(routes.Admin.routes) ~
      logRequestResult("customer-routes")(routes.Customer.routes) ~
      logRequestResult("public-routes")(routes.Public.routes)
    }
  }

  def bind(config: Config = ConfigFactory.parseString(conf)): Future[ServerBinding] = {
    Http().bindAndHandle(allRoutes, config.getString("http.interface"), config.getInt("http.port"))
  }
}
