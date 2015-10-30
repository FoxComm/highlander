package routes.admin

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.Order.orderRefNumRegex
import models.GiftCard.giftCardCodeRegex
import models._
import payloads._
import services._
import services.orders._
import slick.driver.PostgresDriver.api._
import utils.Http._
import utils.Slick.DbResult
import utils.Slick.implicits._
import utils.CustomDirectives._
import utils.{Apis, Slick}

object OrderRoutes {

  def routes(implicit ec: ExecutionContext, db: Database,
    mat: Materializer, storeAdminAuth: AsyncAuthenticator[StoreAdmin], apis: Apis) = {

    authenticateBasicAsync(realm = "admin", storeAdminAuth) { admin ⇒

      pathPrefix("orders") {
        (get & pathEnd & sortAndPage) { implicit sortAndPage ⇒
          good {
            OrderQueries.findAll.run()
          }
        } ~
        (post & entity(as[CreateOrder]) & pathEnd) { payload ⇒
          goodOrFailures { OrderCreator.createCart(payload) }
        } ~
        (patch & pathEnd & sortAndPage) { implicit sortAndPage ⇒
          entity(as[BulkUpdateOrdersPayload]) { payload ⇒
            goodOrFailures {
              OrderUpdater.updateStatuses(payload.referenceNumbers, payload.status)
            }
          }
        } ~
        pathPrefix("assignees") {
          (post & pathEnd & sortAndPage) { implicit sortAndPage ⇒
            entity(as[BulkAssignment]) { payload ⇒
              goodOrFailures {
                BulkOrderUpdater.assign(payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd & sortAndPage) { implicit sortAndPage ⇒
            entity(as[BulkAssignment]) { payload ⇒
              goodOrFailures {
                BulkOrderUpdater.unassign(payload)
              }
            }
          }
        }
      } ~
      pathPrefix("orders" / orderRefNumRegex) { refNum ⇒
        (get & pathEnd) {
          goodOrFailures {
            val finder = Orders.findByRefNum(refNum)
            finder.selectOne { order ⇒
              DbResult.fromDbio(Slick.fullOrder(finder))
            }
          }
        } ~
        (patch & entity(as[UpdateOrderPayload])) { payload ⇒
          goodOrFailures {
            OrderUpdater.updateStatus(refNum, payload.status)
          }
        } ~
        (post & path("increase-remorse-period") & pathEnd) {
          goodOrFailures {
            LockAwareOrderUpdater.increaseRemorsePeriod(refNum)
          }
        } ~
        (post & path("lock") & pathEnd) {
          goodOrFailures {
            LockAwareOrderUpdater.lock(refNum, admin)
          }
        } ~
        (post & path("unlock") & pathEnd) {
          goodOrFailures {
            LockAwareOrderUpdater.unlock(refNum)
          }
        } ~
        (post & path("checkout")) {
          nothingOrFailures {
            Result.unit // FIXME Stubbed until checkout is updated
          }
        } ~
        (post & path("line-items") & entity(as[Seq[UpdateLineItemsPayload]])) { reqItems ⇒
          goodOrFailures {
            LineItemUpdater.updateQuantitiesOnOrder(refNum, reqItems)
          }
        } ~
        (post & path("gift-cards") & entity(as[AddGiftCardLineItem]) & pathEnd) { payload ⇒
          goodOrFailures {
            LineItemUpdater.addGiftCard(refNum, payload)
          }
        } ~
        (patch & path("gift-cards" / giftCardCodeRegex) & entity(as[AddGiftCardLineItem]) & pathEnd) { (code, payload) ⇒
          goodOrFailures {
            LineItemUpdater.editGiftCard(refNum, code, payload)
          }
        } ~
        (delete & path("gift-cards" / giftCardCodeRegex) & pathEnd) { code ⇒
          goodOrFailures {
            LineItemUpdater.deleteGiftCard(refNum, code)
          }
        } ~
        pathPrefix("payment-methods" / "credit-cards") {
          ((post | patch) & entity(as[payloads.CreditCardPayment]) & pathEnd) { payload ⇒
            goodOrFailures { OrderPaymentUpdater.addCreditCard(refNum, payload.creditCardId) }
          } ~
          (delete & pathEnd) {
            goodOrFailures { OrderPaymentUpdater.deleteCreditCard(refNum) }
          }
        } ~
        pathPrefix("payment-methods" / "gift-cards") {
          (post & entity(as[payloads.GiftCardPayment]) & pathEnd) { payload ⇒
            goodOrFailures { OrderPaymentUpdater.addGiftCard(refNum, payload) }
          } ~
          (delete & path(GiftCard.giftCardCodeRegex) & pathEnd) { code ⇒
            goodOrFailures { OrderPaymentUpdater.deleteGiftCard(refNum, code) }
          }
        } ~
        pathPrefix("payment-methods" / "store-credit") {
          (post & entity(as[payloads.StoreCreditPayment]) & pathEnd) { payload ⇒
            goodOrFailures { OrderPaymentUpdater.addStoreCredit(refNum, payload) }
          } ~
          (patch & entity(as[payloads.StoreCreditPayment]) & pathEnd) { payload ⇒
            goodOrFailures { OrderPaymentUpdater.addStoreCredit(refNum, payload) }
          } ~
          (delete & pathEnd) {
            goodOrFailures { OrderPaymentUpdater.deleteStoreCredit(refNum) }
          }
        } ~
        pathPrefix("assignees") {
          (post & entity(as[Assignment])) { payload ⇒
            goodOrFailures {
              LockAwareOrderUpdater.assign(refNum, payload.assignees)
            }
          }
        } ~
        pathPrefix("shipping-address") {
          (post & entity(as[payloads.CreateAddressPayload]) & pathEnd) { payload ⇒
            goodOrFailures {
              OrderUpdater.createShippingAddressFromPayload(payload, refNum)
            }
          } ~
          (patch & entity(as[payloads.UpdateAddressPayload]) & pathEnd) { payload ⇒
            goodOrFailures {
              OrderUpdater.updateShippingAddressFromPayload(payload, refNum)
            }
          } ~
          (patch & path(IntNumber) & pathEnd) { addressId ⇒
            goodOrFailures {
              OrderUpdater.createShippingAddressFromAddressId(addressId, refNum)
            }
          } ~
          (delete & pathEnd) {
            goodOrFailures {
              OrderUpdater.removeShippingAddress(refNum)
            }
          }
        } ~
        pathPrefix("shipping-method") {
          (patch & entity(as[payloads.UpdateShippingMethod]) & pathEnd) { payload ⇒
            goodOrFailures {
              OrderUpdater.updateShippingMethod(payload, refNum)
            }
          } ~
          (delete & pathEnd) {
            goodOrFailures {
              OrderUpdater.deleteShippingMethod(refNum)
            }
          }
        }
      }
    }
  }
}
