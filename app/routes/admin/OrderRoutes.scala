package routes.admin

import akka.http.scaladsl.server.Directives._

import cats.implicits._
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.StoreAdmin
import models.order.Order.orderRefNumRegex
import models.payment.giftcard.GiftCard
import models.payment.giftcard.GiftCard.giftCardCodeRegex
import models.traits.Originator
import payloads.AddressPayloads._
import payloads.LineItemPayloads._
import payloads.OrderPayloads._
import payloads.PaymentPayloads._
import payloads.UpdateShippingMethod
import services.orders._
import services.{Checkout, LineItemUpdater}
import utils.Apis
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.Http._

object OrderRoutes {

  def routes(implicit ec: EC, es: ES, db: DB, admin: StoreAdmin, apis: Apis) = {

    activityContext(admin) { implicit ac ⇒
      determineObjectContext(db, ec) { productContext ⇒
        pathPrefix("orders") {
          (post & pathEnd & entity(as[CreateOrder])) { payload ⇒
            goodOrFailures {
              OrderCreator.createCart(admin, payload, productContext)
            }
          } ~
          (patch & pathEnd & entity(as[BulkUpdateOrdersPayload])) { payload ⇒
            goodOrFailures {
              OrderStateUpdater.updateStates(admin, payload.referenceNumbers, payload.state)
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
            goodOrFailures {
              OrderStateUpdater.updateState(admin, refNum, payload.state)
            }
          } ~
          (post & path("coupon" / Segment) & pathEnd) { code ⇒
            goodOrFailures {
              OrderPromotionUpdater.attachCoupon(Originator(admin),
                                                 refNum.some,
                                                 productContext,
                                                 code)
            }
          } ~
          (delete & path("coupon") & pathEnd) {
            goodOrFailures {
              OrderPromotionUpdater.detachCoupon(Originator(admin), refNum.some)
            }
          } ~
          (post & path("increase-remorse-period") & pathEnd) {
            goodOrFailures {
              OrderUpdater.increaseRemorsePeriod(refNum, admin)
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
            goodOrFailures {
              Checkout.fromCart(refNum)
            }
          } ~
          (post & path("coupon" / Segment) & pathEnd) { code ⇒
            goodOrFailures {
              OrderPromotionUpdater.attachCoupon(Originator(admin),
                                                 Some(refNum),
                                                 productContext,
                                                 code)
            }
          } ~
          (delete & path("coupon") & pathEnd) {
            nothingOrFailures {
              OrderPromotionUpdater.detachCoupon(Originator(admin), Some(refNum))
            }
          } ~
          (post & path("line-items") & pathEnd & entity(as[Seq[UpdateLineItemsPayload]])) {
            reqItems ⇒
              goodOrFailures {
                LineItemUpdater.updateQuantitiesOnOrder(admin, refNum, reqItems)
              }
          } ~
          (post & path("gift-cards") & pathEnd & entity(as[AddGiftCardLineItem])) { payload ⇒
            goodOrFailures {
              LineItemUpdater.addGiftCard(admin, refNum, payload)
            }
          } ~
          (patch & path("gift-cards" / giftCardCodeRegex) & pathEnd & entity(
                  as[AddGiftCardLineItem])) { (code, payload) ⇒
            goodOrFailures {
              LineItemUpdater.editGiftCard(admin, refNum, code, payload)
            }
          } ~
          (delete & path("gift-cards" / giftCardCodeRegex) & pathEnd) { code ⇒
            goodOrFailures {
              LineItemUpdater.deleteGiftCard(admin, refNum, code)
            }
          } ~
          pathPrefix("payment-methods" / "credit-cards") {
            (post & pathEnd & entity(as[CreditCardPayment])) { payload ⇒
              goodOrFailures {
                OrderPaymentUpdater.addCreditCard(Originator(admin),
                                                  payload.creditCardId,
                                                  Some(refNum))
              }
            } ~
            (delete & pathEnd) {
              goodOrFailures {
                OrderPaymentUpdater.deleteCreditCard(Originator(admin), Some(refNum))
              }
            }
          } ~
          pathPrefix("payment-methods" / "gift-cards") {
            (post & pathEnd & entity(as[GiftCardPayment])) { payload ⇒
              goodOrFailures {
                OrderPaymentUpdater.addGiftCard(Originator(admin), payload, Some(refNum))
              }
            } ~
            (patch & pathEnd & entity(as[GiftCardPayment])) { payload ⇒
              goodOrFailures {
                OrderPaymentUpdater.editGiftCard(Originator(admin), payload, Some(refNum))
              }
            } ~
            (delete & path(GiftCard.giftCardCodeRegex) & pathEnd) { code ⇒
              goodOrFailures {
                OrderPaymentUpdater.deleteGiftCard(Originator(admin), code, Some(refNum))
              }
            }
          } ~
          pathPrefix("payment-methods" / "store-credit") {
            (post & pathEnd & entity(as[StoreCreditPayment])) { payload ⇒
              goodOrFailures {
                OrderPaymentUpdater.addStoreCredit(Originator(admin), payload, Some(refNum))
              }
            } ~
            (delete & pathEnd) {
              goodOrFailures {
                OrderPaymentUpdater.deleteStoreCredit(Originator(admin), Some(refNum))
              }
            }
          } ~
          pathPrefix("shipping-address") {
            (post & pathEnd & entity(as[CreateAddressPayload])) { payload ⇒
              goodOrFailures {
                OrderShippingAddressUpdater.createShippingAddressFromPayload(Originator(admin),
                                                                             payload,
                                                                             Some(refNum))
              }
            } ~
            (patch & path(IntNumber) & pathEnd) { addressId ⇒
              goodOrFailures {
                OrderShippingAddressUpdater.createShippingAddressFromAddressId(Originator(admin),
                                                                               addressId,
                                                                               Some(refNum))
              }
            } ~
            (patch & pathEnd & entity(as[UpdateAddressPayload])) { payload ⇒
              goodOrFailures {
                OrderShippingAddressUpdater.updateShippingAddressFromPayload(Originator(admin),
                                                                             payload,
                                                                             Some(refNum))
              }
            } ~
            (delete & pathEnd) {
              goodOrFailures {
                OrderShippingAddressUpdater.removeShippingAddress(Originator(admin), Some(refNum))
              }
            }
          } ~
          pathPrefix("shipping-method") {
            (patch & pathEnd & entity(as[UpdateShippingMethod])) { payload ⇒
              goodOrFailures {
                OrderShippingMethodUpdater.updateShippingMethod(Originator(admin),
                                                                payload,
                                                                Some(refNum))
              }
            } ~
            (delete & pathEnd) {
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
