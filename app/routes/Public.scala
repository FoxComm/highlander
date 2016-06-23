package routes

import akka.http.scaladsl.server.Directives._

import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import models.Reason.reasonTypeRegex
import payloads.CustomerPayloads.CreateCustomerPayload
import services.PublicService._
import services.customers.CustomerManager
import services.giftcards.GiftCardService
import services.orders.OrderPromotionUpdater
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
            goodOrFailures {
              CustomerManager.create(regRequest)
            }
          }
        } ~
        pathPrefix("products") {
          determineObjectContext(db, ec) { implicit productContext ⇒
            pathPrefix(IntNumber) { productId ⇒
              (get & pathEnd) {
                getGoodOrFailures {
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
            goodOrFailures {
              findCountry(countryId)
            }
          }
        } ~
        // TODO move to ES
        pathPrefix("gift-cards" / "types") {
          (get & pathEnd) {
            goodOrFailures {
              GiftCardService.getOriginTypes
            }
          }
        } ~
        // TODO move to ES
        pathPrefix("store-credits" / "types") {
          (get & pathEnd) {
            goodOrFailures {
              StoreCreditService.getOriginTypes
            }
          }
        } ~
        // TODO move to ES
        pathPrefix("reasons" / reasonTypeRegex) { reasonType ⇒
          (get & pathEnd & sortAndPage) { implicit sortAndPage ⇒
            goodOrFailures {
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
