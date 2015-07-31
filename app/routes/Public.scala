package routes

import scala.concurrent.ExecutionContext
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer

import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import models._
import org.json4s.jackson.Serialization.{write ⇒ json}
import responses.PublicSku
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}

object Public {
  def routes(implicit ec: ExecutionContext, db: Database, mat: Materializer) = {
    import Json4sSupport._
    import utils.Http._

    pathPrefix("registrations") {
      (post & path("new") & entity(as[payloads.CreateCustomer])) { regRequest =>
        complete {
          Customers.createFromPayload(regRequest).map(renderGoodOrFailures)
        }
      }
    } ~
    pathPrefix("skus") {
      (get & path(IntNumber)) { skuId =>
        complete {
          renderOrNotFound(PublicSku.findById(skuId))
        }
      }
    }
  }
}
