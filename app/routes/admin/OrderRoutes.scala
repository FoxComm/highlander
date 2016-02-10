package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.order.Order
import models.payment.giftcard.GiftCard
import GiftCard.giftCardCodeRegex
import Order.orderRefNumRegex
import models.StoreAdmin
import models.traits.Originator
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
            OrderQueries.list
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
          (post & pathEnd & entity(as[payloads.CreditCardPayment])) { payload ⇒
            activityContext(admin) { implicit ac ⇒
              goodOrFailures {
                OrderPaymentUpdater.addCreditCard(Originator(admin), payload.creditCardId, Some(refNum))
              }
            }
          } ~
          (delete & pathEnd) {
            activityContext(admin) { implicit ac ⇒
              goodOrFailures {
                OrderPaymentUpdater.deleteCreditCard(Originator(admin), Some(refNum))
              }
            }
          }
        } ~
        pathPrefix("payment-methods" / "gift-cards") {
          (post & pathEnd & entity(as[payloads.GiftCardPayment])) { payload ⇒
            activityContext(admin) { implicit ac ⇒
              goodOrFailures {
                OrderPaymentUpdater.addGiftCard(Originator(admin), payload, Some(refNum))
              }
            }
          } ~
          (delete & path(GiftCard.giftCardCodeRegex) & pathEnd) { code ⇒
            activityContext(admin) { implicit ac ⇒
              goodOrFailures {
                OrderPaymentUpdater.deleteGiftCard(Originator(admin), code, Some(refNum))
              }
            }
          }
        } ~
        pathPrefix("payment-methods" / "store-credit") {
          (post & pathEnd & entity(as[payloads.StoreCreditPayment])) { payload ⇒
            activityContext(admin) { implicit ac ⇒
              goodOrFailures {
                OrderPaymentUpdater.addStoreCredit(Originator(admin), payload, Some(refNum))
              }
            }
          } ~
          (delete & pathEnd) {
            activityContext(admin) { implicit ac ⇒
              goodOrFailures {
                OrderPaymentUpdater.deleteStoreCredit(Originator(admin), Some(refNum))
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
                OrderWatcherUpdater.unwatch(admin, refNum, assigneeId)
              }
            }
          }
        } ~
        pathPrefix("shipping-address") {
          (post & pathEnd & entity(as[payloads.CreateAddressPayload])) { payload ⇒
            activityContext(admin) { implicit ac ⇒
              goodOrFailures {
                OrderShippingAddressUpdater.createShippingAddressFromPayload(Originator(admin), payload, Some(refNum))
              }
            }
          } ~
          (patch & path(IntNumber) & pathEnd) { addressId ⇒
            activityContext(admin) { implicit ac ⇒
              goodOrFailures {
                OrderShippingAddressUpdater.createShippingAddressFromAddressId(Originator(admin), addressId, Some(refNum))
              }
            }
          } ~
          (patch & pathEnd & entity(as[payloads.UpdateAddressPayload])) { payload ⇒
            activityContext(admin) { implicit ac ⇒
              goodOrFailures {
                OrderShippingAddressUpdater.updateShippingAddressFromPayload(Originator(admin), payload, Some(refNum))
              }
            }
          } ~
          (delete & pathEnd) {
            activityContext(admin) { implicit ac ⇒
              goodOrFailures {
                OrderShippingAddressUpdater.removeShippingAddress(Originator(admin), Some(refNum))
              }
            }
          }
        } ~
        pathPrefix("shipping-method") {
          (patch & pathEnd & entity(as[payloads.UpdateShippingMethod])) { payload ⇒
            activityContext(admin) { implicit ac ⇒
              goodOrFailures {
                OrderShippingMethodUpdater.updateShippingMethod(Originator(admin), payload, Some(refNum))
              }
            }
          } ~
          (delete & pathEnd) {
            activityContext(admin) { implicit ac ⇒
              goodOrFailures {
                OrderShippingMethodUpdater.deleteShippingMethod(Originator(admin), Some(refNum))
              }
            }
          }
        }
      }
    }
  }
}
