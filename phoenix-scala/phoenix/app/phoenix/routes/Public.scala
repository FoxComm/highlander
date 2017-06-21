package phoenix.routes

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.github.tminglei.slickpg.LTree
import phoenix.models.Reason.reasonTypeRegex
import phoenix.models.location.Region
import phoenix.payloads.CustomerPayloads.CreateCustomerPayload
import phoenix.services.PublicService._
import phoenix.services.account.AccountCreateContext
import phoenix.services.customers.CustomerManager
import phoenix.services.giftcards.GiftCardService
import phoenix.services.product.ProductManager
import phoenix.services.{ReasonService, StoreCreditService}
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis
import phoenix.utils.http.CustomDirectives._
import phoenix.utils.http.Http._
import phoenix.utils.http.JsonSupport._

object Public {
  def routes(customerCreateContext: AccountCreateContext,
             defaultScope: LTree)(implicit ec: EC, db: DB, apis: Apis): Route =
    activityContext(defaultScope) { implicit ac ⇒
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
                  ProductManager.getProduct(productId, checkActive = true)
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
          } ~
          (get & path(Region.regionCodeRegex) & pathEnd) { shortName ⇒
            getOrFailures {
              findRegionByShortName(shortName)
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
            mutateOrFailures {
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
