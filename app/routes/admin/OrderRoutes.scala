package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.GiftCard.giftCardCodeRegex
import models.Order.orderRefNumRegex
import models.{GiftCard, StoreAdmin}
import payloads.{AddGiftCardLineItem, Assignment, BulkAssignment, BulkUpdateOrdersPayload, CreateOrder,
UpdateLineItemsPayload, UpdateOrderPayload, Watchers, BulkWatchers}
import services.orders._
import services.{Checkout, LineItemUpdater}
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives._
import utils.Http._
import utils.Apis

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
          activityContext(admin) { implicit ac ⇒
            goodOrFailures {
              OrderCreator.createCart(admin, payload)
            }
          }
        } ~
        (patch & pathEnd & sortAndPage) { implicit sortAndPage ⇒
          entity(as[BulkUpdateOrdersPayload]) { payload ⇒
            activityContext(admin) { implicit ac ⇒
              goodOrFailures {
                OrderStateUpdater.updateStates(admin, payload.referenceNumbers, payload.state)
              }
            }
          }
        } ~
        pathPrefix("assignees") {
          (post & pathEnd & sortAndPage) { implicit sortAndPage ⇒
            entity(as[BulkAssignment]) { payload ⇒
              activityContext(admin) { implicit ac ⇒
                goodOrFailures {
                  OrderAssignmentUpdater.assignBulk(admin, payload)
                }
              }
            }
          } ~
          (post & path("delete") & pathEnd & sortAndPage) { implicit sortAndPage ⇒
            entity(as[BulkAssignment]) { payload ⇒
              activityContext(admin) { implicit ac ⇒
                goodOrFailures {
                  OrderAssignmentUpdater.unassignBulk(admin, payload)
                }
              }
            }
          }
        } ~
        pathPrefix("watchers") {
          (post & pathEnd & sortAndPage) { implicit sortAndPage ⇒
            entity(as[BulkWatchers]) { payload ⇒
              activityContext(admin) { implicit ac ⇒
                goodOrFailures {
                  OrderWatcherUpdater.watchBulk(admin, payload)
                }
              }
            }
          } ~
          (post & path("delete") & pathEnd & sortAndPage) { implicit sortAndPage ⇒
            entity(as[BulkWatchers]) { payload ⇒
              activityContext(admin) { implicit ac ⇒
                goodOrFailures {
                  OrderWatcherUpdater.unwatchBulk(admin, payload)
                }
              }
            }
          }
        }
      } ~
      pathPrefix("orders" / orderRefNumRegex) { refNum ⇒
        (get & pathEnd) {
          goodOrFailures {
            OrderQueries.findOne(refNum)
          }
        } ~
        (patch & pathEnd & entity(as[UpdateOrderPayload])) { payload ⇒
          activityContext(admin) { implicit ac ⇒
            goodOrFailures {
              OrderStateUpdater.updateState(admin, refNum, payload.state)
            }
          }
        } ~
        (post & path("increase-remorse-period") & pathEnd) {
          activityContext(admin) { implicit ac ⇒
            goodOrFailures {
              OrderUpdater.increaseRemorsePeriod(refNum, admin)
            }
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
          activityContext(admin) { implicit ac ⇒
            goodOrFailures {
              Checkout.fromCart(refNum)
            }
          }
        } ~
        (post & path("line-items") & pathEnd & entity(as[Seq[UpdateLineItemsPayload]])) { reqItems ⇒
          activityContext(admin) { implicit ac ⇒
            goodOrFailures {
              LineItemUpdater.updateQuantitiesOnOrder(admin, refNum, reqItems)
            }
          }
        } ~
        (post & path("gift-cards") & pathEnd & entity(as[AddGiftCardLineItem])) { payload ⇒
          activityContext(admin) { implicit ac ⇒
            goodOrFailures {
              LineItemUpdater.addGiftCard(admin, refNum, payload)
            }
          }
        } ~
        (patch & path("gift-cards" / giftCardCodeRegex) & pathEnd & entity(as[AddGiftCardLineItem])) { (code, payload) ⇒
          activityContext(admin) { implicit ac ⇒
            goodOrFailures {
              LineItemUpdater.editGiftCard(admin, refNum, code, payload)
            }
          }
        } ~
        (delete & path("gift-cards" / giftCardCodeRegex) & pathEnd) { code ⇒
          activityContext(admin) { implicit ac ⇒
            goodOrFailures {
              LineItemUpdater.deleteGiftCard(admin, refNum, code)
            }
          }
        } ~
        pathPrefix("payment-methods" / "credit-cards") {
          ((post | patch) & pathEnd & entity(as[payloads.CreditCardPayment])) { payload ⇒
            activityContext(admin) { implicit ac ⇒
              goodOrFailures {
                OrderPaymentUpdater.addCreditCard(admin, refNum, payload.creditCardId)
              }
            }
          } ~
          (delete & pathEnd) {
            activityContext(admin) { implicit ac ⇒
              goodOrFailures {
                OrderPaymentUpdater.deleteCreditCard(admin, refNum)
              }
            }
          }
        } ~
        pathPrefix("payment-methods" / "gift-cards") {
          (post & pathEnd & entity(as[payloads.GiftCardPayment])) { payload ⇒
            activityContext(admin) { implicit ac ⇒
              goodOrFailures {
                OrderPaymentUpdater.addGiftCard(admin, refNum, payload)
              }
            }
          } ~
          (delete & path(GiftCard.giftCardCodeRegex) & pathEnd) { code ⇒
            activityContext(admin) { implicit ac ⇒
              goodOrFailures {
                OrderPaymentUpdater.deleteGiftCard(admin, refNum, code)
              }
            }
          }
        } ~
        pathPrefix("payment-methods" / "store-credit") {
          ((post|patch) & pathEnd & entity(as[payloads.StoreCreditPayment])) { payload ⇒
            activityContext(admin) { implicit ac ⇒
              goodOrFailures {
                OrderPaymentUpdater.addStoreCredit(admin, refNum, payload)
              }
            }
          } ~
          (delete & pathEnd) {
            activityContext(admin) { implicit ac ⇒
              goodOrFailures {
                OrderPaymentUpdater.deleteStoreCredit(admin, refNum)
              }
            }
          }
        } ~
        pathPrefix("assignees") {
          (post & pathEnd & entity(as[Assignment])) { payload ⇒
            activityContext(admin) { implicit ac ⇒
              goodOrFailures {
                OrderAssignmentUpdater.assign(admin, refNum, payload.assignees)
              }
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            activityContext(admin) { implicit ac ⇒
              goodOrFailures {
                OrderAssignmentUpdater.unassign(admin, refNum, assigneeId)
              }
            }
          }
        } ~
        pathPrefix("watchers") {
          (post & pathEnd & entity(as[Watchers])) { payload ⇒
            activityContext(admin) { implicit ac ⇒
              goodOrFailures {
                OrderWatcherUpdater.watch(admin, refNum, payload.watchers)
              }
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            activityContext(admin) { implicit ac ⇒
              goodOrFailures {
                OrderWatcherUpdater.unassign(admin, refNum, assigneeId)
              }
            }
          }
        } ~
        pathPrefix("shipping-address") {
          (post & pathEnd & entity(as[payloads.CreateAddressPayload])) { payload ⇒
            activityContext(admin) { implicit ac ⇒
              goodOrFailures {
                OrderShippingAddressUpdater.createShippingAddressFromPayload(admin, payload, refNum)
              }
            }
          } ~
          (patch & path(IntNumber) & pathEnd) { addressId ⇒
            activityContext(admin) { implicit ac ⇒
              goodOrFailures {
                OrderShippingAddressUpdater.createShippingAddressFromAddressId(admin, addressId, refNum)
              }
            }
          } ~
          (patch & pathEnd & entity(as[payloads.UpdateAddressPayload])) { payload ⇒
            activityContext(admin) { implicit ac ⇒
              goodOrFailures {
                OrderShippingAddressUpdater.updateShippingAddressFromPayload(admin, payload, refNum)
              }
            }
          } ~
          (delete & pathEnd) {
            activityContext(admin) { implicit ac ⇒
              goodOrFailures {
                OrderShippingAddressUpdater.removeShippingAddress(admin, refNum)
              }
            }
          }
        } ~
        pathPrefix("shipping-method") {
          (patch & pathEnd & entity(as[payloads.UpdateShippingMethod])) { payload ⇒
            activityContext(admin) { implicit ac ⇒
              goodOrFailures {
                OrderShippingMethodUpdater.updateShippingMethod(admin, payload, refNum)
              }
            }
          } ~
          (delete & pathEnd) {
            activityContext(admin) { implicit ac ⇒
              goodOrFailures {
                OrderShippingMethodUpdater.deleteShippingMethod(admin, refNum)
              }
            }
          }
        }
      }
    }
  }
}
