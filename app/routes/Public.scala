package routes

import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import de.heikoseeberger.akkahttpjson4s.Json4sSupport

import models.Reason.reasonTypeRegex
import payloads._
import services.customers.CustomerManager
import services.giftcards.GiftCardService
import slick.driver.PostgresDriver.api._
import services.{ReasonService, StoreCreditService}
import services.PublicService._
import utils.CustomDirectives._
import utils.aliases._

object Public {
  def routes(implicit ec: EC, db: DB, mat: Materializer) = {
    import Json4sSupport._
    import utils.Http._

    activityContext() { implicit ac ⇒
      pathPrefix("public") {
        pathPrefix("registrations") {
          (post & path("new") & pathEnd & entity(as[CreateCustomerPayload])) { regRequest ⇒
            goodOrFailures {
              CustomerManager.create(regRequest)
            }
          }
        } ~
        pathPrefix("regions") {
          (get & pathEnd) {
            good {
              listRegions
            }
          }
        } ~
        pathPrefix("countries") {
          (get & pathEnd) {
            good {
              listCountries
            }
          } ~
          (get & path(IntNumber) & pathEnd) { countryId ⇒
            goodOrFailures {
              findCountry(countryId)
            }
          }
        } ~
        pathPrefix("gift-cards" / "types") {
          (get & pathEnd) {
            goodOrFailures {
              GiftCardService.getOriginTypes
            }
          }
        } ~
        pathPrefix("store-credits" / "types") {
          (get & pathEnd) {
            goodOrFailures {
              StoreCreditService.getOriginTypes
            }
          }
        } ~
        pathPrefix("reasons") {
          (get & pathEnd & sortAndPage) { implicit sortAndPage ⇒
            goodOrFailures {
              ReasonService.listReasons
            }
          }
        } ~
        pathPrefix("reasons" / reasonTypeRegex) { reasonType ⇒
          (get & pathEnd & sortAndPage) { implicit sortAndPage ⇒
            goodOrFailures {
              ReasonService.listReasonsByType(reasonType)
            }
          }
        } ~
        pathPrefix("rma-reasons") {
          (get & pathEnd & sortAndPage) { implicit sortAndPage ⇒
            goodOrFailures {
              ReasonService.listRmaReasons
            }
          }
        }
      }
    }
  }
}
