package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.StoreAdmin
import services.{CustomerCreditConverter, GiftCardAdjustmentsService, GiftCardService}
import slick.driver.PostgresDriver.api._
import utils.Apis
import utils.CustomDirectives._
import utils.Http._

import scala.concurrent.ExecutionContext

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
        (get & path("types") & pathEnd) {
          goodOrFailures {
            GiftCardService.getOriginTypes
          }
        } ~
        (patch & pathEnd & entity(as[payloads.GiftCardBulkUpdateStatusByCsr])) { payload ⇒
          activityContext(admin) { implicit ac ⇒
            goodOrFailures {
              GiftCardService.bulkUpdateStatusByCsr(payload, admin)
            }
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
        (post & pathEnd & entity(as[payloads.GiftCardBulkCreateByCsr])) { payload ⇒
          activityContext(admin) { implicit ac ⇒
            goodOrFailures {
              GiftCardService.createBulkByAdmin(admin, payload)
            }
          }
        } ~
        (post & pathEnd & entity(as[payloads.GiftCardCreateByCsr])) { payload ⇒
          activityContext(admin) { implicit ac ⇒
            goodOrFailures {
              GiftCardService.createByAdmin(admin, payload)
            }
          }
        } ~
        (patch & path(Segment) & pathEnd & entity(as[payloads.GiftCardUpdateStatusByCsr])) { (code, payload) ⇒
          activityContext(admin) { implicit ac ⇒
            goodOrFailures {
              GiftCardService.updateStatusByCsr(code, payload, admin)
            }
          }
        } ~
        path(Segment / "convert" / IntNumber) { (code, customerId) ⇒
          (post & pathEnd) {
            activityContext(admin) { implicit ac ⇒
              goodOrFailures {
                CustomerCreditConverter.toStoreCredit(code, customerId, admin)
              }
            }
          }
        }
      }
    }
  }
}
