package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import utils.http.JsonSupport._
import models.account.User
import models.returns.Return
import payloads.ReturnPayloads._
import services.returns._
import services.Authenticator.AuthData
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.Http._

object ReturnRoutes {

  def routes(implicit ec: EC, db: DB, auth: AuthData[User]): Route = {

    activityContext(auth.model) { implicit ac ⇒
      determineObjectContext(db, ec) { productContext ⇒
        pathPrefix("returns") {
          (post & pathEnd & entity(as[ReturnCreatePayload])) { payload ⇒
            mutateOrFailures {
              ReturnService.createByAdmin(auth.model, payload)
            }
          }
        } ~
        pathPrefix("returns" / Return.returnRefNumRegex) { refNum ⇒
          (get & pathEnd) {
            getOrFailures {
              ReturnService.getByRefNum(refNum)
            }
          } ~
          (get & path("expanded") & pathEnd) {
            getOrFailures {
              ReturnService.getExpandedByRefNum(refNum)
            }
          } ~
          (patch & pathEnd & entity(as[ReturnUpdateStatePayload])) { payload ⇒
            mutateOrFailures {
              ReturnService.updateStateByCsr(refNum, payload)
            }
          } ~
          (post & path("message") & pathEnd & entity(as[ReturnMessageToCustomerPayload])) {
            payload ⇒
              mutateOrFailures {
                ReturnService.updateMessageToCustomer(refNum, payload)
              }
          } ~
          pathPrefix("line-items" / "skus") {
            (post & pathEnd & entity(as[ReturnSkuLineItemsPayload])) { payload ⇒
              mutateOrFailures {
                ReturnLineItemUpdater.addSkuLineItem(refNum, payload, productContext)
              }
            } ~
            (delete & path(IntNumber) & pathEnd) { lineItemId ⇒
              mutateOrFailures {
                ReturnLineItemUpdater.deleteSkuLineItem(refNum, lineItemId)
              }
            }
          } ~
          pathPrefix("line-items" / "shipping-costs") {
            (post & pathEnd & entity(as[ReturnShippingCostLineItemsPayload])) { payload ⇒
              mutateOrFailures {
                ReturnLineItemUpdater.addShippingCostItem(refNum, payload)
              }
            } ~
            (delete & path(IntNumber) & pathEnd) { lineItemId ⇒
              mutateOrFailures {
                ReturnLineItemUpdater.deleteShippingCostLineItem(refNum, lineItemId)
              }
            }
          } ~
          pathPrefix("payment-methods" / "credit-cards") {
            (post & pathEnd & entity(as[ReturnPaymentPayload])) { payload ⇒
              mutateOrFailures {
                ReturnPaymentUpdater.addCreditCard(refNum, payload)
              }
            } ~
            (delete & pathEnd) {
              mutateOrFailures {
                ReturnPaymentUpdater.deleteCreditCard(refNum)
              }
            }
          } ~
          pathPrefix("payment-methods" / "gift-cards") {
            (post & pathEnd & entity(as[ReturnPaymentPayload])) { payload ⇒
              mutateOrFailures {
                ReturnPaymentUpdater.addGiftCard(refNum, payload)
              }
            } ~
            (delete & pathEnd) {
              mutateOrFailures {
                ReturnPaymentUpdater.deleteGiftCard(refNum)
              }
            }
          } ~
          pathPrefix("payment-methods" / "store-credit") {
            (post & pathEnd & entity(as[ReturnPaymentPayload])) { payload ⇒
              mutateOrFailures {
                ReturnPaymentUpdater.addStoreCredit(refNum, payload)
              }
            } ~
            (delete & pathEnd) {
              mutateOrFailures {
                ReturnPaymentUpdater.deleteStoreCredit(refNum)
              }
            }
          }
        }
      }
    }
  }
}
