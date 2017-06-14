package phoenix.routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{PathMatcher, Route}
import phoenix.models.account.User
import phoenix.models.cord.Cord
import phoenix.models.payment.PaymentMethod
import phoenix.models.returns.Return
import phoenix.payloads.ReturnPayloads._
import phoenix.services.Authenticator.AuthData
import phoenix.services.returns._
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis
import phoenix.utils.http.CustomDirectives._
import phoenix.utils.http.Http._
import phoenix.utils.http.JsonSupport._

object ReturnRoutes {
  val PaymentMethodMatcher = PathMatcher(PaymentMethod.Type)

  def routes(implicit ec: EC, db: DB, auth: AuthData[User], apis: Apis): Route = {

    activityContext(auth) { implicit ac ⇒
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
          (post & path("message") & pathEnd & entity(as[ReturnMessageToCustomerPayload])) { payload ⇒
            mutateOrFailures {
              ReturnService.updateMessageToCustomer(refNum, payload)
            }
          } ~
          pathPrefix("line-items") {
            (post & path("skus") & entity(as[List[ReturnSkuLineItemPayload]])) { payload ⇒
              mutateOrFailures {
                ReturnLineItemManager.updateSkuLineItems(refNum, payload)
              }
            } ~
            (post & pathEnd & entity(as[ReturnLineItemPayload])) { payload ⇒
              mutateOrFailures {
                ReturnLineItemManager.addLineItem(refNum, payload)
              }
            } ~
            (delete & path(IntNumber) & pathEnd) { lineItemId ⇒
              deleteOrFailures {
                ReturnLineItemManager.deleteLineItem(refNum, lineItemId)
              }
            }
          } ~
          pathPrefix("payment-methods") {
            (post & pathEnd & entity(as[ReturnPaymentsPayload])) { payload ⇒
              mutateOrFailures {
                ReturnPaymentManager.updatePayments(refNum, payload.payments, overwrite = true)
              }
            } ~
            (post & path(PaymentMethodMatcher) & pathEnd & entity(as[ReturnPaymentPayload])) {
              case (paymentMethod, payload) ⇒
                mutateOrFailures {
                  ReturnPaymentManager.updatePayments(
                    refNum,
                    Map(paymentMethod → payload.amount),
                    overwrite = false
                  )
                }
            } ~
            (delete & path(PaymentMethodMatcher) & pathEnd) { paymentMethod ⇒
              deleteOrFailures {
                ReturnPaymentManager.deletePayment(refNum, paymentMethod)
              }
            }
          }
        }
      }
    }
  }
}
