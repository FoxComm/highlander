package routes.admin

import akka.http.scaladsl.server.Directives._

import cats.implicits._
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.StoreAdmin
import models.cord.Cord.cordRefNumRegex
import models.payment.giftcard.GiftCard
import models.payment.giftcard.GiftCard.giftCardCodeRegex
import models.traits.Originator
import payloads.AddressPayloads._
import payloads.LineItemPayloads._
import payloads.OrderPayloads._
import payloads.PaymentPayloads._
import payloads.UpdateShippingMethod
import services.carts._
import services.orders._
import services.{Checkout, LineItemUpdater}
import utils.aliases._
import utils.apis.Apis
import utils.http.CustomDirectives._
import utils.http.Http._

object OrderRoutes {

  def routes(implicit ec: EC, es: ES, db: DB, admin: StoreAdmin, apis: Apis) = {

    activityContext(admin) { implicit ac ⇒
      determineObjectContext(db, ec) { implicit ctx ⇒
        pathPrefix("orders") {
          (post & pathEnd & entity(as[CreateCart])) { payload ⇒
            mutateOrFailures {
              CartCreator.createCart(admin, payload)
            }
          } ~
          (patch & pathEnd & entity(as[BulkUpdateOrdersPayload])) { payload ⇒
            mutateOrFailures {
              OrderStateUpdater.updateStates(admin, payload.referenceNumbers, payload.state)
            }
          }
        } ~
        pathPrefix("carts" / cordRefNumRegex) { refNum ⇒
          (get & pathEnd) {
            getOrFailures {
              CartQueries.findOne(refNum)
            }
          }
        } ~
        pathPrefix("orders" / cordRefNumRegex) { refNum ⇒
          (get & pathEnd) {
            getOrFailures {
              OrderQueries.findOne(refNum)
            }
          } ~
          (patch & pathEnd & entity(as[UpdateOrderPayload])) { payload ⇒
            mutateOrFailures {
              OrderStateUpdater.updateState(admin, refNum, payload.state)
            }
          } ~
          (post & path("coupon" / Segment) & pathEnd) { code ⇒
            mutateOrFailures {
              CartPromotionUpdater.attachCoupon(Originator(admin), refNum.some, code)
            }
          } ~
          (delete & path("coupon") & pathEnd) {
            mutateOrFailures {
              CartPromotionUpdater.detachCoupon(Originator(admin), refNum.some)
            }
          } ~
          (post & path("increase-remorse-period") & pathEnd) {
            mutateOrFailures {
              OrderUpdater.increaseRemorsePeriod(refNum, admin)
            }
          } ~
          (post & path("lock") & pathEnd) {
            mutateOrFailures {
              CartLockUpdater.lock(refNum, admin)
            }
          } ~
          (post & path("unlock") & pathEnd) {
            mutateOrFailures {
              CartLockUpdater.unlock(refNum)
            }
          } ~
          (post & path("checkout")) {
            mutateOrFailures {
              Checkout.fromCart(refNum)
            }
          } ~
          (post & path("coupon" / Segment) & pathEnd) { code ⇒
            mutateOrFailures {
              CartPromotionUpdater.attachCoupon(Originator(admin), Some(refNum), code)
            }
          } ~
          (delete & path("coupon") & pathEnd) {
            deleteOrFailures {
              CartPromotionUpdater.detachCoupon(Originator(admin), Some(refNum))
            }
          } ~
          (post & path("line-items") & pathEnd & entity(as[Seq[UpdateLineItemsPayload]])) {
            reqItems ⇒
              mutateOrFailures {
                LineItemUpdater.updateQuantitiesOnCart(admin, refNum, reqItems)
              }
          } ~
          pathPrefix("payment-methods" / "credit-cards") {
            (post & pathEnd & entity(as[CreditCardPayment])) { payload ⇒
              mutateOrFailures {
                CartPaymentUpdater.addCreditCard(Originator(admin),
                                                 payload.creditCardId,
                                                 refNum.some)
              }
            } ~
            (delete & pathEnd) {
              mutateOrFailures {
                CartPaymentUpdater.deleteCreditCard(Originator(admin), refNum.some)
              }
            }
          } ~
          pathPrefix("payment-methods" / "gift-cards") {
            (post & pathEnd & entity(as[GiftCardPayment])) { payload ⇒
              mutateOrFailures {
                CartPaymentUpdater.addGiftCard(Originator(admin), payload, refNum.some)
              }
            } ~
            (patch & pathEnd & entity(as[GiftCardPayment])) { payload ⇒
              mutateOrFailures {
                CartPaymentUpdater.editGiftCard(Originator(admin), payload, refNum.some)
              }
            } ~
            (delete & path(GiftCard.giftCardCodeRegex) & pathEnd) { code ⇒
              mutateOrFailures {
                CartPaymentUpdater.deleteGiftCard(Originator(admin), code, refNum.some)
              }
            }
          } ~
          pathPrefix("payment-methods" / "store-credit") {
            (post & pathEnd & entity(as[StoreCreditPayment])) { payload ⇒
              mutateOrFailures {
                CartPaymentUpdater.addStoreCredit(Originator(admin), payload, refNum.some)
              }
            } ~
            (delete & pathEnd) {
              mutateOrFailures {
                CartPaymentUpdater.deleteStoreCredit(Originator(admin), refNum.some)
              }
            }
          } ~
          pathPrefix("shipping-address") {
            (post & pathEnd & entity(as[CreateAddressPayload])) { payload ⇒
              mutateOrFailures {
                CartShippingAddressUpdater.createShippingAddressFromPayload(Originator(admin),
                                                                            payload,
                                                                            Some(refNum))
              }
            } ~
            (patch & path(IntNumber) & pathEnd) { addressId ⇒
              mutateOrFailures {
                CartShippingAddressUpdater.createShippingAddressFromAddressId(Originator(admin),
                                                                              addressId,
                                                                              Some(refNum))
              }
            } ~
            (patch & pathEnd & entity(as[UpdateAddressPayload])) { payload ⇒
              mutateOrFailures {
                CartShippingAddressUpdater.updateShippingAddressFromPayload(Originator(admin),
                                                                            payload,
                                                                            Some(refNum))
              }
            } ~
            (delete & pathEnd) {
              mutateOrFailures {
                CartShippingAddressUpdater.removeShippingAddress(Originator(admin), Some(refNum))
              }
            }
          } ~
          pathPrefix("shipping-method") {
            (patch & pathEnd & entity(as[UpdateShippingMethod])) { payload ⇒
              mutateOrFailures {
                CartShippingMethodUpdater.updateShippingMethod(Originator(admin),
                                                               payload,
                                                               Some(refNum))
              }
            } ~
            (delete & pathEnd) {
              mutateOrFailures {
                CartShippingMethodUpdater.deleteShippingMethod(Originator(admin), Some(refNum))
              }
            }
          }
        }
      }
    }
  }
}
