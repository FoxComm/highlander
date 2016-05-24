package routes.admin

import akka.http.scaladsl.server.Directives._
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.order.Order
import models.rma.{Rmas, Rma}
import models.StoreAdmin
import payloads.RmaPayloads._
import services.rmas._
import utils.http.CustomDirectives._
import utils.http.Http._
import utils.aliases._

object RmaRoutes {

  def routes(implicit ec: EC, db: DB, admin: StoreAdmin) = {

    activityContext(admin) { implicit ac ⇒
      determineObjectContext(db, ec) { productContext ⇒
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
                RmaPaymentUpdater.addGiftCard(refNum, payload)
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
                RmaPaymentUpdater.addStoreCredit(refNum, payload)
              }
            } ~
            (delete & pathEnd) {
              goodOrFailures {
                RmaPaymentUpdater.deleteStoreCredit(refNum)
              }
            }
          }
        }
      }
    }
  }
}
