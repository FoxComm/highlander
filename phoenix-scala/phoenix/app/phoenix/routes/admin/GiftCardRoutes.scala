package phoenix.routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cats.implicits._
import core.db._
import phoenix.models.account.User
import phoenix.models.payment.giftcard.GiftCard.giftCardCodeRegex
import phoenix.payloads.GiftCardPayloads._
import phoenix.services.Authenticator.AuthData
import phoenix.services.CustomerCreditConverter
import phoenix.services.giftcards._
import phoenix.utils.apis.Apis
import phoenix.utils.http.CustomDirectives._
import phoenix.utils.http.Http._
import phoenix.utils.http.JsonSupport._

object GiftCardRoutes {

  def routes(implicit ec: EC, db: DB, auth: AuthData[User], apis: Apis): Route =
    activityContext(auth) { implicit ac ⇒
      pathPrefix("customer-gift-cards") {
        path("bulk") {
          (post & pathEnd & entity(as[Seq[GiftCardCreatedByCustomer]])) { payload ⇒
            mutateOrFailures {
              DbResultT.seqCollectFailures(
                payload.map(GiftCardService.createByCustomer(auth.model, _)).toList)
            }
          }
        } ~
        (post & pathEnd & entity(as[GiftCardCreatedByCustomer])) { payload ⇒
          mutateOrFailures {
            GiftCardService.createByCustomer(auth.model, payload)
          }
        }
      } ~
      pathPrefix("gift-cards") {
        path("bulk") {
          (post & pathEnd & entity(as[GiftCardBulkCreateByCsr])) { payload ⇒
            mutateOrFailures {
              GiftCardService.createBulkByAdmin(auth.model, payload)
            }
          } ~
          (patch & pathEnd & entity(as[GiftCardBulkUpdateStateByCsr])) { payload ⇒
            mutateOrFailures {
              GiftCardService.bulkUpdateStateByCsr(payload, auth.model)
            }
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
