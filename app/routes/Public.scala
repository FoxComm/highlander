package routes

import akka.http.scaladsl.server.Directives._

import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import models.Reason.reasonTypeRegex
import payloads.CustomerPayloads.CreateCustomerPayload
import services.PublicService._
import services.customers.CustomerManager
import services.giftcards.GiftCardService
import services.product.ProductManager
import services.{ReasonService, StoreCreditService}
import utils.aliases._
import utils.http.CustomDirectives._

object Public {
  def routes(implicit ec: EC, db: DB, es: ES) = {
    import Json4sSupport._
    import utils.http.Http._

    activityContext() { implicit ac ⇒
      pathPrefix("public") {
        pathPrefix("registrations") {
          (post & path("new") & pathEnd & entity(as[CreateCustomerPayload])) { regRequest ⇒
            mutateOrFailures {
              CustomerManager.create(regRequest)
            }
          }
        } ~
        pathPrefix("products") {
          determineObjectContext(db, ec) { implicit productContext ⇒
            pathPrefix(IntNumber) { productId ⇒
              (get & pathEnd) {
                getOrFailures {
                  ProductManager.getProduct(productId)
                }
              }
            }
          }
        } ~
        // TODO move to ES
        pathPrefix("regions") {
          (get & pathEnd) {
            good {
              listRegions
            }
          }
        } ~
        // TODO move to ES
        pathPrefix("countries") {
          (get & pathEnd) {
            good {
              listCountries
            }
          } ~
          (get & path(IntNumber) & pathEnd) { countryId ⇒
            getOrFailures {
              findCountry(countryId)
            }
          }
        } ~
        pathPrefix("gift-cards" / "types") {
          (get & pathEnd) {
            getOrFailures {
              GiftCardService.getOriginTypes
            }
          }
        } ~
        pathPrefix("store-credits" / "types") {
          (get & pathEnd) {
            getOrFailures {
              StoreCreditService.getOriginTypes
            }
          }
        } ~
        pathPrefix("reasons" / reasonTypeRegex) { reasonType ⇒
          (get & pathEnd) {
            getOrFailures {
              ReasonService.listReasonsByType(reasonType)
            }
          }
        } ~
        pathPrefix("ping") {
          (get & pathEnd) {
            good(render("pong"))
          }
        }
      }
    }
  }
}
