package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.order.Order
import models.rma.{Rmas, Rma}
import models.StoreAdmin
import payloads.{AssignmentPayload, BulkAssignmentPayload}
import services.assignments.{RmaAssignmentsManager, RmaWatchersManager}
import payloads.{RmaCreatePayload, RmaGiftCardLineItemsPayload, RmaMessageToCustomerPayload, RmaPaymentPayload,
RmaShippingCostLineItemsPayload, RmaSkuLineItemsPayload, RmaUpdateStatePayload}
import services.rmas._
import utils.Apis
import utils.CustomDirectives._
import utils.Http._
import utils.aliases._

object RmaRoutes {

  def routes(implicit ec: EC, db: DB, mat: Materializer, admin: StoreAdmin, apis: Apis) = {

    activityContext(admin) { implicit ac ⇒
      determineProductContext(db, ec) { productContext ⇒ 

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
              entity(as[BulkAssignmentPayload[String]]) { payload ⇒
                goodOrFailures {
                  RmaAssignmentsManager.assignBulk(admin, payload)
                }
              }
            } ~
            (post & path("delete") & pathEnd & sortAndPage) { implicit sortAndPage ⇒
              entity(as[BulkAssignmentPayload[String]]) { payload ⇒
                goodOrFailures {
                  RmaAssignmentsManager.unassignBulk(admin, payload)
                }
              }
            }
          } ~
          pathPrefix("watchers") {
            (post & pathEnd & sortAndPage) { implicit sortAndPage ⇒
              entity(as[BulkAssignmentPayload[String]]) { payload ⇒
                goodOrFailures {
                  RmaWatchersManager.assignBulk(admin, payload)
                }
              }
            } ~
            (post & path("delete") & pathEnd & sortAndPage) { implicit sortAndPage ⇒
              entity(as[BulkAssignmentPayload[String]]) { payload ⇒
                goodOrFailures {
                  RmaWatchersManager.unassignBulk(admin, payload)
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
          (patch & pathEnd & entity(as[RmaUpdateStatePayload])) { payload ⇒
            goodOrFailures {
              RmaService.updateStateByCsr(refNum, payload)
            }
          } ~
          (post & path("message") & pathEnd & entity(as[RmaMessageToCustomerPayload])) { payload ⇒
            goodOrFailures {
              RmaService.updateMessageToCustomer(refNum, payload)
            }
          } ~
          (get & path("lock") & pathEnd) {
            goodOrFailures {
              RmaLockUpdater.getLockState(refNum)
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
                RmaLineItemUpdater.addSkuLineItem(refNum, payload, productContext)
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
            (post & pathEnd & entity(as[RmaPaymentPayload])) { payload ⇒
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
            (post & pathEnd & entity(as[RmaPaymentPayload])) { payload ⇒
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
            (post & pathEnd & entity(as[RmaPaymentPayload])) { payload ⇒
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
            (post & entity(as[AssignmentPayload])) { payload ⇒
              goodOrFailures {
                RmaAssignmentsManager.assign(refNum, payload, admin)
              }
            } ~
            (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
              goodOrFailures {
                RmaAssignmentsManager.unassign(refNum, assigneeId, admin)
              }
            }
          } ~
          pathPrefix("watchers") {
            (post & entity(as[AssignmentPayload])) { payload ⇒
              goodOrFailures {
                RmaWatchersManager.assign(refNum, payload, admin)
              }
            } ~
            (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
              goodOrFailures {
                RmaWatchersManager.unassign(refNum, assigneeId, admin)
              }
            }
          }
        }
      }
    }
  }
}

