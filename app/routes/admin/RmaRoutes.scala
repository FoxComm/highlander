package routes.admin

import java.time.Instant

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer

import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._

import models._
import payloads._
import services.{LineItemUpdater, RmaService}
import responses.StoreAdminResponse
import responses.RmaResponse._
import slick.driver.PostgresDriver.api._
import utils.Apis
import utils.Http._
import utils.CustomDirectives._

object RmaRoutes {

  def routes(implicit ec: ExecutionContext, db: Database,
    mat: Materializer, storeAdminAuth: AsyncAuthenticator[StoreAdmin], apis: Apis) = {

    authenticateBasicAsync(realm = "admin", storeAdminAuth) { admin ⇒
      val adminResponse = Some(StoreAdminResponse.build(admin))
      val genericRmaMock = buildMockRma(id = 1, refNum = "ABC-123", orderId = 1, admin = adminResponse)

      pathPrefix("rmas") {
        (get & pathEnd & sortAndPage) { implicit sortAndPage ⇒
          goodOrFailures {
            RmaService.findAll
          }
        } ~
        (get & path("customer" / IntNumber)) { customerId ⇒
          (pathEnd & sortAndPage) { implicit sortAndPage ⇒
            goodOrFailures {
              RmaService.findByCustomerId(customerId)
            }
          }
        } ~
        (get & path("order" / Order.orderRefNumRegex)) { refNum ⇒
          (pathEnd & sortAndPage) { implicit sortAndPage ⇒
            goodOrFailures {
              RmaService.findByOrderRef(refNum)
            }
          }
        } ~
        (post & entity(as[RmaCreatePayload]) & pathEnd) { payload ⇒
          good {
            genericRmaMock.copy(orderId = payload.orderId, orderRefNum = payload.orderRefNum)
          }
        }
      } ~
      pathPrefix("rmas" / Rma.rmaRefNumRegex) { refNum ⇒
        (get & pathEnd) {
          goodOrFailures {
            RmaService.getByRefNum(refNum)
          }
        } ~
        (get & path("expanded") & pathEnd) {
          goodOrFailures {
            RmaService.getExpandedByRefNum(refNum)
          }
        } ~
        (patch & entity(as[RmaUpdateStatusPayload]) & pathEnd) { payload ⇒
          good {
            genericRmaMock.copy(status = payload.status)
          }
        } ~
        (post & path("lock") & pathEnd) {
          good {
            genericRmaMock
          }
        } ~
        (post & path("unlock") & pathEnd) {
          good {
            genericRmaMock
          }
        } ~
        (post & path("line-items") & entity(as[Seq[RmaSkuLineItemsPayload]])) { reqItems ⇒
          good {
            genericRmaMock
          }
        } ~
        (post & path("gift-cards") & entity(as[Seq[RmaGiftCardLineItemsPayload]])) { reqItems ⇒
          good {
            genericRmaMock
          }
        } ~
        (post & path("shipping-costs") & entity(as[Seq[RmaShippingCostLineItemsPayload]])) { reqItems ⇒
          good {
            genericRmaMock
          }
        } ~
        pathPrefix("payment-methods" / "credit-cards") {
          ((post | patch) & entity(as[payloads.RmaCreditCardPayment]) & pathEnd) { payload ⇒
            good {
              genericRmaMock
            }
          } ~
          (delete & pathEnd) {
            good {
              genericRmaMock
            }
          }
        } ~
        pathPrefix("payment-methods" / "gift-cards") {
          (post & entity(as[payloads.RmaGiftCardPayment]) & pathEnd) { payload ⇒
            good {
              genericRmaMock
            }
          } ~
          (delete & path(GiftCard.giftCardCodeRegex) & pathEnd) { code ⇒
            good {
              genericRmaMock
            }
          }
        } ~
        pathPrefix("payment-methods" / "store-credit") {
          ((post | patch) & entity(as[payloads.RmaStoreCreditPayment]) & pathEnd) { payload ⇒
            good {
              genericRmaMock
            }
          } ~
          (delete & pathEnd) {
            good {
              genericRmaMock
            }
          }
        }
      }
    }
  }
}