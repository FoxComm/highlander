package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._

import models.StoreAdmin
import payloads._
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
          (post & pathEnd & entity(as[GiftCardBulkCreateByCsr])) { payload ⇒
            goodOrFailures {
              GiftCardService.createBulkByAdmin(admin, payload)
            }
          } ~
          (post & pathEnd & entity(as[GiftCardCreateByCsr])) { payload ⇒
            goodOrFailures {
              GiftCardService.createByAdmin(admin, payload)
            }
          } ~
          (patch & path(Segment) & pathEnd & entity(as[GiftCardUpdateStateByCsr])) { (code, payload) ⇒
            goodOrFailures {
              GiftCardService.updateStateByCsr(code, payload, admin)
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
}
