package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.{Order, Rma, Rmas, StoreAdmin}
import payloads.{RmaAssigneesPayload, RmaBulkAssigneesPayload, RmaCreatePayload, RmaGiftCardLineItemsPayload,
RmaMessageToCustomerPayload, RmaPaymentPayload, RmaShippingCostLineItemsPayload, RmaSkuLineItemsPayload, RmaUpdateStatusPayload}
import services.rmas._
import slick.driver.PostgresDriver.api._
import utils.Apis
import utils.CustomDirectives._
import utils.Http._

import scala.concurrent.ExecutionContext

object RmaRoutes {

  def routes(implicit ec: ExecutionContext, db: Database,
    mat: Materializer, storeAdminAuth: AsyncAuthenticator[StoreAdmin], apis: Apis) = {

    authenticateBasicAsync(realm = "admin", storeAdminAuth) { admin ⇒
      pathPrefix("rmas") {
        (get & pathEnd & sortAndPage) { implicit sortAndPage ⇒
          goodOrFailures {
            RmaQueries.findAll(Rmas)
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
        (post & pathEnd & entity(as[RmaCreatePayload])) { payload ⇒
          goodOrFailures {
            RmaService.createByAdmin(admin, payload)
          }
        } ~
        pathPrefix("assignees") {
          (post & pathEnd & sortAndPage) { implicit sortAndPage ⇒
            entity(as[RmaBulkAssigneesPayload]) { payload ⇒
              goodOrFailures {
                RmaAssignmentUpdater.assign(payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd & sortAndPage) { implicit sortAndPage ⇒
            entity(as[RmaBulkAssigneesPayload]) { payload ⇒
              goodOrFailures {
                RmaAssignmentUpdater.unassign(payload)
              }
            }
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
        (patch & pathEnd & entity(as[RmaUpdateStatusPayload])) { payload ⇒
          goodOrFailures {
            RmaService.updateStatusByCsr(refNum, payload)
          }
        } ~
        (post & path("message") & pathEnd & entity(as[RmaMessageToCustomerPayload])) { payload ⇒
          goodOrFailures {
            RmaService.updateMessageToCustomer(refNum, payload)
          }
        } ~
        (get & path("lock") & pathEnd) {
          goodOrFailures {
            RmaLockUpdater.getLockStatus(refNum)
          }
        } ~
        (post & path("lock") & pathEnd) {
          goodOrFailures {
            RmaLockUpdater.lock(refNum, admin)
          }
        } ~
        (post & path("unlock") & pathEnd) {
          goodOrFailures {
            RmaLockUpdater.unlock(refNum)
          }
        } ~
        pathPrefix("line-items" / "skus") {
          (post & pathEnd & entity(as[RmaSkuLineItemsPayload])) { payload ⇒
            goodOrFailures {
              RmaLineItemUpdater.addSkuLineItem(refNum, payload)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { lineItemId ⇒
            goodOrFailures {
              RmaLineItemUpdater.deleteSkuLineItem(refNum, lineItemId)
            }
          }
        } ~
        pathPrefix("line-items" / "gift-cards") {
          (post & pathEnd & entity(as[RmaGiftCardLineItemsPayload])) { payload ⇒
            goodOrFailures {
              RmaLineItemUpdater.addGiftCardLineItem(refNum, payload)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { lineItemId ⇒
            goodOrFailures {
              RmaLineItemUpdater.deleteGiftCardLineItem(refNum, lineItemId)
            }
          }
        } ~
        pathPrefix("line-items" / "shipping-costs") {
          (post & pathEnd & entity(as[RmaShippingCostLineItemsPayload])) { payload ⇒
            goodOrFailures {
              RmaLineItemUpdater.addShippingCostItem(refNum, payload)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { lineItemId ⇒
            goodOrFailures {
              RmaLineItemUpdater.deleteShippingCostLineItem(refNum, lineItemId)
            }
          }
        } ~
        pathPrefix("payment-methods" / "credit-cards") {
          ((post | patch) & pathEnd & entity(as[RmaPaymentPayload])) { payload ⇒
            goodOrFailures {
              RmaPaymentUpdater.addCreditCard(refNum, payload)
            }
          } ~
          (delete & pathEnd) {
            goodOrFailures {
              RmaPaymentUpdater.deleteCreditCard(refNum)
            }
          }
        } ~
        pathPrefix("payment-methods" / "gift-cards") {
          ((post | patch) & pathEnd & entity(as[RmaPaymentPayload])) { payload ⇒
            goodOrFailures {
              RmaPaymentUpdater.addGiftCard(admin, refNum, payload)
            }
          } ~
          (delete & pathEnd) {
            goodOrFailures {
              RmaPaymentUpdater.deleteGiftCard(refNum)
            }
          }
        } ~
        pathPrefix("payment-methods" / "store-credit") {
          ((post | patch) & pathEnd & entity(as[RmaPaymentPayload])) { payload ⇒
            goodOrFailures {
              RmaPaymentUpdater.addStoreCredit(admin, refNum, payload)
            }
          } ~
          (delete & pathEnd) {
            goodOrFailures {
              RmaPaymentUpdater.deleteStoreCredit(refNum)
            }
          }
        } ~
        pathPrefix("assignees") {
          (post & entity(as[RmaAssigneesPayload])) { payload ⇒
            goodOrFailures {
              RmaAssignmentUpdater.assign(refNum, payload.assignees)
            }
          }
        }
      }
    }
  }
}