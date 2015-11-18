package routes.admin

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._

import models._
import payloads._
import services.rmas._

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
      val genericRmaMock = buildMockRma(id = 1, refNum = "ABC-123", admin = adminResponse)

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
        (patch & entity(as[RmaUpdateStatusPayload]) & pathEnd) { payload ⇒
          good {
            genericRmaMock.copy(status = payload.status)
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
        (post & path("line-items") & pathEnd & entity(as[Seq[RmaSkuLineItemsPayload]])) { reqItems ⇒
          good {
            genericRmaMock
          }
        } ~
        (post & path("gift-cards") & pathEnd & entity(as[Seq[RmaGiftCardLineItemsPayload]])) { reqItems ⇒
          good {
            genericRmaMock
          }
        } ~
        (post & path("shipping-costs") & pathEnd & entity(as[Seq[RmaShippingCostLineItemsPayload]])) { reqItems ⇒
          good {
            genericRmaMock
          }
        } ~
        pathPrefix("payment-methods" / "credit-cards") {
          ((post | patch) & pathEnd & entity(as[payloads.RmaCcPaymentPayload])) { payload ⇒
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
          ((post | patch) & pathEnd & entity(as[payloads.RmaPaymentPayload])) { payload ⇒
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
          ((post | patch) & pathEnd & entity(as[payloads.RmaPaymentPayload])) { payload ⇒
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