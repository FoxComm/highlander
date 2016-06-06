package routes.admin

import akka.http.scaladsl.server.Directives._

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.StoreAdmin
import models.returns.Return
import payloads.ReturnPayloads._
import services.returns._
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.Http._

object ReturnRoutes {

  def routes(implicit ec: EC, db: DB, admin: StoreAdmin) = {

    activityContext(admin) { implicit ac ⇒
      determineObjectContext(db, ec) { productContext ⇒
        pathPrefix("returns") {
          (post & pathEnd & entity(as[ReturnCreatePayload])) { payload ⇒
            goodOrFailures {
              ReturnService.createByAdmin(admin, payload)
            }
          }
        } ~
        pathPrefix("returns" / Return.returnRefNumRegex) { refNum ⇒
          (get & pathEnd) {
            goodOrFailures {
              ReturnService.getByRefNum(refNum)
            }
          } ~
          (get & path("expanded") & pathEnd) {
            goodOrFailures {
              ReturnService.getExpandedByRefNum(refNum)
            }
          } ~
          (patch & pathEnd & entity(as[ReturnUpdateStatePayload])) { payload ⇒
            goodOrFailures {
              ReturnService.updateStateByCsr(refNum, payload)
            }
          } ~
          (post & path("message") & pathEnd & entity(as[ReturnMessageToCustomerPayload])) {
            payload ⇒
              goodOrFailures {
                ReturnService.updateMessageToCustomer(refNum, payload)
              }
          } ~
          (get & path("lock") & pathEnd) {
            goodOrFailures {
              ReturnLockUpdater.getLockState(refNum)
            }
          } ~
          (post & path("lock") & pathEnd) {
            goodOrFailures {
              ReturnLockUpdater.lock(refNum, admin)
            }
          } ~
          (post & path("unlock") & pathEnd) {
            goodOrFailures {
              ReturnLockUpdater.unlock(refNum)
            }
          } ~
          pathPrefix("line-items" / "skus") {
            (post & pathEnd & entity(as[ReturnSkuLineItemsPayload])) { payload ⇒
              goodOrFailures {
                ReturnLineItemUpdater.addSkuLineItem(refNum, payload, productContext)
              }
            } ~
            (delete & path(IntNumber) & pathEnd) { lineItemId ⇒
              goodOrFailures {
                ReturnLineItemUpdater.deleteSkuLineItem(refNum, lineItemId)
              }
            }
          } ~
          pathPrefix("line-items" / "gift-cards") {
            (post & pathEnd & entity(as[ReturnGiftCardLineItemsPayload])) { payload ⇒
              goodOrFailures {
                ReturnLineItemUpdater.addGiftCardLineItem(refNum, payload)
              }
            } ~
            (delete & path(IntNumber) & pathEnd) { lineItemId ⇒
              goodOrFailures {
                ReturnLineItemUpdater.deleteGiftCardLineItem(refNum, lineItemId)
              }
            }
          } ~
          pathPrefix("line-items" / "shipping-costs") {
            (post & pathEnd & entity(as[ReturnShippingCostLineItemsPayload])) { payload ⇒
              goodOrFailures {
                ReturnLineItemUpdater.addShippingCostItem(refNum, payload)
              }
            } ~
            (delete & path(IntNumber) & pathEnd) { lineItemId ⇒
              goodOrFailures {
                ReturnLineItemUpdater.deleteShippingCostLineItem(refNum, lineItemId)
              }
            }
          } ~
          pathPrefix("payment-methods" / "credit-cards") {
            (post & pathEnd & entity(as[ReturnPaymentPayload])) { payload ⇒
              goodOrFailures {
                ReturnPaymentUpdater.addCreditCard(refNum, payload)
              }
            } ~
            (delete & pathEnd) {
              goodOrFailures {
                ReturnPaymentUpdater.deleteCreditCard(refNum)
              }
            }
          } ~
          pathPrefix("payment-methods" / "gift-cards") {
            (post & pathEnd & entity(as[ReturnPaymentPayload])) { payload ⇒
              goodOrFailures {
                ReturnPaymentUpdater.addGiftCard(refNum, payload)
              }
            } ~
            (delete & pathEnd) {
              goodOrFailures {
                ReturnPaymentUpdater.deleteGiftCard(refNum)
              }
            }
          } ~
          pathPrefix("payment-methods" / "store-credit") {
            (post & pathEnd & entity(as[ReturnPaymentPayload])) { payload ⇒
              goodOrFailures {
                ReturnPaymentUpdater.addStoreCredit(refNum, payload)
              }
            } ~
            (delete & pathEnd) {
              goodOrFailures {
                ReturnPaymentUpdater.deleteStoreCredit(refNum)
              }
            }
          }
        }
      }
    }
  }
}
