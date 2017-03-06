package routes.admin

import akka.http.scaladsl.server.Directives.{path, pathPrefix, _}
import akka.http.scaladsl.server.{PathMatcher, Route}
import utils.http.JsonSupport._
import models.account.User
import models.cord.Cord
import models.payment.PaymentMethod
import models.returns.Return
import payloads.ReturnPayloads._
import services.returns._
import services.Authenticator.AuthData
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.Http._

object ReturnRoutes {
  val PaymentMethodMatcher = PathMatcher(PaymentMethod.Type.typeMap)

  def routes(implicit ec: EC, db: DB, auth: AuthData[User]): Route = {

    activityContext(auth.model) { implicit ac ⇒
      determineObjectContext(db, ec) { implicit productContext ⇒
        pathPrefix("returns") {
          (post & pathEnd & entity(as[ReturnCreatePayload])) { payload ⇒
            mutateOrFailures {
              ReturnService.createByAdmin(auth.model, payload)
            }
          } ~
          (get & pathEnd) {
            getOrFailures {
              ReturnService.list
            }
          } ~
          pathPrefix("customer") {
            (get & path(IntNumber) & pathEnd) { customerId ⇒
              getOrFailures {
                ReturnService.getByCustomer(customerId)
              }
            }
          } ~
          pathPrefix("order" / Cord.cordRefNumRegex) { refNum ⇒
            (get & pathEnd) {
              getOrFailures {
                ReturnService.getByOrder(refNum)
              }
            }
          } ~
          pathPrefix("reasons") {
            (get & pathEnd) {
              getOrFailures {
                ReturnReasonsManager.reasonsList
              }
            } ~
            (post & pathEnd & entity(as[ReturnReasonPayload])) { payload ⇒
              mutateOrFailures {
                ReturnReasonsManager.addReason(payload)
              }
            } ~
            (delete & path(IntNumber) & pathEnd) { id ⇒
              deleteOrFailures {
                ReturnReasonsManager.deleteReason(id)
              }
            }
          }
        } ~
        pathPrefix("returns" / Return.returnRefNumRegex) { refNum ⇒
          (get & pathEnd) {
            getOrFailures {
              ReturnService.getByRefNum(refNum)
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
          pathPrefix("line-items") {
            (post & pathEnd & entity(as[ReturnLineItemPayload])) { payload ⇒
              mutateOrFailures {
                ReturnLineItemUpdater.addLineItem(refNum, payload)
              }
            } ~
            (delete & path(IntNumber) & pathEnd) { lineItemId ⇒
              mutateOrFailures {
                ReturnLineItemUpdater.deleteLineItem(refNum, lineItemId)
              }
            }
          } ~
          pathPrefix("payment-methods") {
            (post & pathEnd & entity(as[ReturnPaymentsPayload])) { payload ⇒
              mutateOrFailures {
                ReturnPaymentUpdater.addPayments(refNum, payload)
              }
            } ~
            (post & path(PaymentMethodMatcher) & pathEnd & entity(as[ReturnPaymentPayload])) {
              case (paymentMethod, payload) ⇒
                mutateOrFailures {
                  ReturnPaymentUpdater.addPayment(refNum, paymentMethod, payload)
                }
            } ~
            (delete & path(PaymentMethodMatcher) & pathEnd) { paymentMethod ⇒
              mutateOrFailures {
                ReturnPaymentUpdater.deletePayment(refNum, paymentMethod)
              }
            }
          }
        }
      }
    }
  }
}
