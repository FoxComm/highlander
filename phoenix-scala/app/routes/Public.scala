package routes

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse}
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.CookieDirectives.{setCookie ⇒ _, _}
import akka.http.scaladsl.server.directives.RespondWithDirectives.{respondWithHeader ⇒ _, _}
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.Reason.reasonTypeRegex
import payloads.CustomerPayloads.CreateCustomerPayload
import services.PublicService._
import services.account.AccountCreateContext
import services.customers.CustomerManager
import services.giftcards.GiftCardService
import services.product.ProductManager
import services.{JwtCookie, ReasonService, StoreCreditService}
import utils.http.CustomDirectives._
import utils.aliases._
import utils.db._
import org.json4s.jackson.Serialization.{write ⇒ json}
import utils.http.Http._

object Public {
  def routes(customerCreateContext: AccountCreateContext)(implicit ec: EC, db: DB, es: ES) = {

    activityContext() { implicit ac ⇒
      pathPrefix("public") {
        pathPrefix("registrations") {
          (post & path("new") & pathEnd & entity(as[CreateCustomerPayload])) { payload ⇒
            onSuccess(CustomerManager
                  .create(payload = payload, context = customerCreateContext)
                  .runTxn) { result ⇒
              result.fold({ f ⇒
                complete(renderFailure(f))
              }, { resp ⇒
                {
                  val (body, auth) = resp
                  respondWithHeader(RawHeader("JWT", auth.jwt)).&(setCookie(JwtCookie(auth))) {
                    complete(HttpResponse(
                            entity = HttpEntity(ContentTypes.`application/json`, json(body))
                        ))
                  }
                }
              })
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
