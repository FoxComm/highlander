package routes.admin


import scala.concurrent.ExecutionContext
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer

import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models._
import services._
import slick.driver.PostgresDriver.api._
import utils.Apis
import utils.Http._
import utils.Slick.implicits._
import utils.CustomDirectives._

object GiftCardRoutes {

  def routes(implicit ec: ExecutionContext, db: Database,
    mat: Materializer, storeAdminAuth: AsyncAuthenticator[StoreAdmin], apis: Apis) = {

    authenticateBasicAsync(realm = "admin", storeAdminAuth) { admin ⇒

      pathPrefix("gift-cards") {
        (get & pathEnd & sortAndPage) { implicit sortAndPage ⇒
          goodOrFailures {
            GiftCardService.findAll
          }
        } ~
        (patch & entity(as[payloads.GiftCardBulkUpdateStatusByCsr]) & pathEnd) { payload ⇒
          goodOrFailures {
            GiftCardService.bulkUpdateStatusByCsr(payload, admin)
          }
        } ~
        (get & path(Segment) & pathEnd) { code ⇒
          goodOrFailures {
            GiftCardService.getByCode(code)
          }
        } ~
        (get & path(Segment / "transactions")) { code ⇒
          (pathEnd & sortAndPage) { implicit sortAndPage ⇒
            goodOrFailures {
              GiftCardAdjustmentsService.forGiftCard(code)
            }
          }
        } ~
        (post & entity(as[payloads.GiftCardBulkCreateByCsr]) & pathEnd) { payload ⇒
          goodOrFailures {
            GiftCardService.createBulkByAdmin(admin, payload)
          }
        } ~
        (post & entity(as[payloads.GiftCardCreateByCsr]) & pathEnd) { payload ⇒
          goodOrFailures {
            GiftCardService.createByAdmin(admin, payload)
          }
        } ~
        (patch & path(Segment) & entity(as[payloads.GiftCardUpdateStatusByCsr]) & pathEnd) { (code, payload) ⇒
          goodOrFailures {
            GiftCardService.updateStatusByCsr(code, payload, admin)
          }
        } ~
        path(Segment / "convert" / IntNumber) { (code, customerId) ⇒
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
