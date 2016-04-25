package routes.admin

import akka.http.scaladsl.server.Directives._
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.StoreAdmin
import models.payment.giftcard.GiftCard.giftCardCodeRegex
import payloads._
import services.giftcards._
import services.CustomerCreditConverter
import utils.http.CustomDirectives._
import utils.http.Http._
import utils.aliases._

object GiftCardRoutes {

  def routes(implicit ec: EC, db: DB, admin: StoreAdmin) = {

    activityContext(admin) { implicit ac ⇒
      pathPrefix("gift-cards") {
        (get & pathEnd & sortAndPage) { implicit sortAndPage ⇒
          goodOrFailures {
            GiftCardService.findAll
          }
        } ~
        (patch & pathEnd & entity(as[GiftCardBulkUpdateStateByCsr])) { payload ⇒
          goodOrFailures {
            GiftCardService.bulkUpdateStateByCsr(payload, admin)
          }
        } ~
        (post & pathEnd & entity(as[GiftCardBulkCreateByCsr])) { payload ⇒
          goodOrFailures {
            GiftCardService.createBulkByAdmin(admin, payload)
          }
        } ~
        (post & pathEnd & entity(as[GiftCardCreateByCsr])) { payload ⇒
          goodOrFailures {
            GiftCardService.createByAdmin(admin, payload)
          }
        }
      } ~
      pathPrefix("gift-cards" / giftCardCodeRegex) { code ⇒
        (get & pathEnd) {
          goodOrFailures {
            GiftCardService.getByCode(code)
          }
        } ~
        (patch & pathEnd & entity(as[GiftCardUpdateStateByCsr])) { payload ⇒
          goodOrFailures {
            GiftCardService.updateStateByCsr(code, payload, admin)
          }
        } ~
        path("transactions") {
          (get & pathEnd & sortAndPage) { implicit sortAndPage ⇒
            goodOrFailures {
              GiftCardAdjustmentsService.forGiftCard(code)
            }
          }
        } ~
        path("convert" / IntNumber) { customerId ⇒
          (post & pathEnd) {
            goodOrFailures {
              CustomerCreditConverter.toStoreCredit(code, customerId, admin)
            }
          }
        }
      }
    }
  }
}
