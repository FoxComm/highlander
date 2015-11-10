package routes

import scala.concurrent.ExecutionContext
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer

import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import models._
import org.json4s.jackson.Serialization.{write ⇒ json}
import responses.PublicSku
import slick.driver.PostgresDriver.api._
import services.CustomerManager
import utils.CustomDirectives._

object Public {
  def routes(implicit ec: ExecutionContext, db: Database, mat: Materializer) = {
    import Json4sSupport._
    import utils.Http._

    pathPrefix("registrations") {
      (post & path("new") & pathEnd & entity(as[payloads.CreateCustomerPayload])) { regRequest ⇒
         goodOrFailures {
          CustomerManager.create(regRequest)
        }
      }
    } ~
    pathPrefix("skus") {
      (get & path(IntNumber)) { skuId ⇒
        complete {
          renderOrNotFound(PublicSku.findById(skuId))
        }
      }
    } ~
    (get & path("countries") & pathEnd) {
      complete {
        services.Public.countries.map(render(_))
      }
    } ~
    (get & path("regions") & pathEnd) {
      complete {
        services.Public.regions.map(render(_))
      }
    } ~
    (get & path("countries" / IntNumber) & pathEnd) { countryId ⇒
      complete {
        services.Public.findCountry(countryId).map(renderGoodOrFailures(_))
      }
    }
  }
}
