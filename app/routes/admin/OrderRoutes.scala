package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.GiftCard.giftCardCodeRegex
import models.Order.orderRefNumRegex
import models.{GiftCard, Orders, StoreAdmin}
import payloads.{AddGiftCardLineItem, Assignment, BulkAssignment, BulkUpdateOrdersPayload, CreateOrder, UpdateLineItemsPayload, UpdateOrderPayload}
import services.orders._
import services.{LineItemUpdater, Result}
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives._
import utils.Http._
import utils.Slick.DbResult
import utils.{Apis, Slick}

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext

object OrderRoutes {

  def routes(implicit ec: ExecutionContext, db: Database,
    mat: Materializer, storeAdminAuth: AsyncAuthenticator[StoreAdmin], apis: Apis) = {

    authenticateBasicAsync(realm = "admin", storeAdminAuth) { admin ⇒

      pathPrefix("orders") {
        (get & pathEnd & sortAndPage) { implicit sortAndPage ⇒
          goodOrFailures {
            OrderQueries.findAll
          }
        } ~
        (post & pathEnd & entity(as[CreateOrder])) { payload ⇒
          goodOrFailures { OrderCreator.createCart(payload) }
        } ~
        (patch & pathEnd & sortAndPage) { implicit sortAndPage ⇒
          entity(as[BulkUpdateOrdersPayload]) { payload ⇒
            goodOrFailures {
              OrderStatusUpdater.updateStatuses(payload.referenceNumbers, payload.status)
            }
          }
        } ~
        pathPrefix("assignees") {
          (post & pathEnd & sortAndPage) { implicit sortAndPage ⇒
            entity(as[BulkAssignment]) { payload ⇒
              goodOrFailures {
                OrderAssignmentUpdater.assign(payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd & sortAndPage) { implicit sortAndPage ⇒
            entity(as[BulkAssignment]) { payload ⇒
              goodOrFailures {
                OrderAssignmentUpdater.unassign(payload)
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
        (patch & pathEnd & entity(as[UpdateOrderPayload])) { payload ⇒
          goodOrFailures {
            OrderStatusUpdater.updateStatus(refNum, payload.status)
          }
        } ~
        (post & path("increase-remorse-period") & pathEnd) {
          goodOrFailures {
            OrderUpdater.increaseRemorsePeriod(refNum)
          }
        } ~
        (post & path("lock") & pathEnd) {
          goodOrFailures {
            OrderLockUpdater.lock(refNum, admin)
          }
        } ~
        (post & path("unlock") & pathEnd) {
          goodOrFailures {
            OrderLockUpdater.unlock(refNum)
          }
        } ~
        (post & path("checkout")) {
          nothingOrFailures {
            Result.unit // FIXME Stubbed until checkout is updated
          }
        } ~
        (post & path("line-items") & pathEnd & entity(as[Seq[UpdateLineItemsPayload]])) { reqItems ⇒
          goodOrFailures {
            LineItemUpdater.updateQuantitiesOnOrder(refNum, reqItems)
          }
        } ~
        (post & path("gift-cards") & pathEnd & entity(as[AddGiftCardLineItem])) { payload ⇒
          goodOrFailures {
            LineItemUpdater.addGiftCard(refNum, payload)
          }
        } ~
        (patch & path("gift-cards" / giftCardCodeRegex) & pathEnd & entity(as[AddGiftCardLineItem])) { (code, payload) ⇒
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
          ((post | patch) & pathEnd & entity(as[payloads.CreditCardPayment])) { payload ⇒
            goodOrFailures { OrderPaymentUpdater.addCreditCard(refNum, payload.creditCardId) }
          } ~
          (delete & pathEnd) {
            goodOrFailures { OrderPaymentUpdater.deleteCreditCard(refNum) }
          }
        } ~
        pathPrefix("payment-methods" / "gift-cards") {
          (post & pathEnd & entity(as[payloads.GiftCardPayment])) { payload ⇒
            goodOrFailures { OrderPaymentUpdater.addGiftCard(refNum, payload) }
          } ~
          (delete & path(GiftCard.giftCardCodeRegex) & pathEnd) { code ⇒
            goodOrFailures { OrderPaymentUpdater.deleteGiftCard(refNum, code) }
          }
        } ~
        pathPrefix("payment-methods" / "store-credit") {
          ((post|patch) & pathEnd & entity(as[payloads.StoreCreditPayment])) { payload ⇒
            goodOrFailures { OrderPaymentUpdater.addStoreCredit(refNum, payload) }
          } ~
          (delete & pathEnd) {
            goodOrFailures { OrderPaymentUpdater.deleteStoreCredit(refNum) }
          }
        } ~
        pathPrefix("assignees") {
          (post & pathEnd & entity(as[Assignment])) { payload ⇒
            goodOrFailures {
              OrderAssignmentUpdater.assign(refNum, payload.assignees)
            }
          }
        } ~
        pathPrefix("shipping-address") {
          (post & pathEnd & entity(as[payloads.CreateAddressPayload])) { payload ⇒
            goodOrFailures {
              OrderShippingAddressUpdater.createShippingAddressFromPayload(payload, refNum)
            }
          } ~
          (patch & pathEnd & entity(as[payloads.UpdateAddressPayload])) { payload ⇒
            goodOrFailures {
              OrderShippingAddressUpdater.updateShippingAddressFromPayload(payload, refNum)
            }
          } ~
          (patch & path(IntNumber) & pathEnd) { addressId ⇒
            goodOrFailures {
              OrderShippingAddressUpdater.createShippingAddressFromAddressId(addressId, refNum)
            }
          } ~
          (delete & pathEnd) {
            goodOrFailures {
              OrderShippingAddressUpdater.removeShippingAddress(refNum)
            }
          }
        } ~
        pathPrefix("shipping-method") {
          (patch & pathEnd & entity(as[payloads.UpdateShippingMethod])) { payload ⇒
            goodOrFailures {
              OrderShippingMethodUpdater.updateShippingMethod(payload, refNum)
            }
          } ~
          (delete & pathEnd) {
            goodOrFailures {
              OrderShippingMethodUpdater.deleteShippingMethod(refNum)
            }
          }
        }
      }
    }
  }
}
