package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.account.User
import models.payment.giftcard.GiftCard.giftCardCodeRegex
import payloads.GiftCardPayloads._
import services.CustomerCreditConverter
import services.giftcards._
import services.Authenticator.AuthData
import utils.aliases._
import utils.db.DbResultT
import utils.http.CustomDirectives._
import utils.http.Http._

import com.github.levkhomich.akka.tracing.TracingExtensionImpl

object GiftCardRoutes {

  def routes(implicit ec: EC,
             db: DB,
             auth: AuthData[User],
             tr: TracingRequest,
             trace: TracingExtensionImpl): Route = {

    activityContext(auth.model) { implicit ac ⇒
      path("customer-gift-cards") {
        (post & pathEnd & entity(as[GiftCardCreatedByCustomer])) { payload ⇒
          mutateOrFailures {
            GiftCardService.createByCustomer(auth.model, payload)
          }
        } ~
        (post & pathEnd & entity(as[Seq[GiftCardCreatedByCustomer]])) { payload ⇒
          mutateOrFailures {
            DbResultT.sequence(payload.map(GiftCardService.createByCustomer(auth.model, _)))
          }
        }
      } ~
      pathPrefix("gift-cards") {
        (patch & pathEnd & entity(as[GiftCardBulkUpdateStateByCsr])) { payload ⇒
          mutateOrFailures {
            GiftCardService.bulkUpdateStateByCsr(payload, auth.model)
          }
        } ~
        (post & pathEnd & entity(as[GiftCardBulkCreateByCsr])) { payload ⇒
          mutateOrFailures {
            GiftCardService.createBulkByAdmin(auth.model, payload)
          }
        } ~
        (post & pathEnd & entity(as[GiftCardCreateByCsr])) { payload ⇒
          mutateOrFailures {
            GiftCardService.createByAdmin(auth.model, payload)
          }
        }
      } ~
      pathPrefix("gift-cards" / giftCardCodeRegex) { code ⇒
        (get & pathEnd) {
          getOrFailures {
            GiftCardService.getByCode(code)
          }
        } ~
        (patch & pathEnd & entity(as[GiftCardUpdateStateByCsr])) { payload ⇒
          mutateOrFailures {
            GiftCardService.updateStateByCsr(code, payload, auth.model)
          }
        } ~
        path("transactions") {
          (get & pathEnd) {
            getOrFailures {
              GiftCardAdjustmentsService.forGiftCard(code)
            }
          }
        } ~
        path("convert" / IntNumber) { customerId ⇒
          (post & pathEnd) {
            mutateOrFailures {
              CustomerCreditConverter.toStoreCredit(code, customerId, auth.model)
            }
          }
        }
      }
    }
  }
}
