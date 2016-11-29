package routes

import akka.http.scaladsl.server.Directives._

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.Reason.reasonTypeRegex
import payloads.CustomerPayloads.CreateCustomerPayload
import services.PublicService._
import services.account.AccountCreateContext
import services.customers.CustomerManager
import services.giftcards.GiftCardService
import services.product.ProductManager
import services.{ReasonService, StoreCreditService}
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.Http._

object Public {

  def routes(customerCreateContext: AccountCreateContext)(implicit ec: EC, db: DB, es: ES) = {

    activityContext() { implicit ac ⇒
      pathPrefix("public") {
        pathPrefix("registrations") {
          (post & path("new") & pathEnd & entity(as[CreateCustomerPayload])) { payload ⇒
            mutateWithNewTokenOrFailures {
              CustomerManager.create(payload = payload, context = customerCreateContext)
            }
          }
        } ~
        pathPrefix("products") {
          determineObjectContext(db, ec) { implicit productContext ⇒
            pathPrefix(ProductRef) { productId ⇒
              (get & pathEnd) {
                getOrFailures {
                  ProductManager.getProduct(productId)
                }
              }
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
        pathPrefix("ping") {
          (get & pathEnd) {
            complete(renderPlain("pong"))
          }
        }
      }
    }
  }
}
