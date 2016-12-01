package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cats.implicits._
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.account.User
import models.cord.Cord.cordRefNumRegex
import models.payment.giftcard.GiftCard
import payloads.AddressPayloads._
import payloads.CartPayloads._
import payloads.LineItemPayloads._
import payloads.OrderPayloads._
import payloads.PaymentPayloads._
import payloads.UpdateShippingMethod
import services.carts._
import services.orders._
import services.{Checkout, LineItemUpdater}
import services.Authenticator.AuthData
import utils.aliases._
import utils.apis.Apis
import utils.http.CustomDirectives._
import utils.http.Http._

object OrderRoutes {

  def routes(implicit ec: EC, es: ES, db: DB, auth: AuthData[User], apis: Apis): Route = {

    activityContext(auth.model) { implicit ac ⇒
      determineObjectContext(db, ec) { implicit ctx ⇒
        pathPrefix("orders") {
          // depreciated in favor of /carts route
          (post & pathEnd & entity(as[CreateCart])) { payload ⇒
            mutateOrFailures {
              CartCreator.createCart(auth.model, payload)
            }
          } ~
          (patch & pathEnd & entity(as[BulkUpdateOrdersPayload])) { payload ⇒
            mutateOrFailures {
              OrderStateUpdater.updateStates(auth.model, payload.referenceNumbers, payload.state)
            }
          }
        } ~
        pathPrefix("orders" / cordRefNumRegex) { refNum ⇒
          (get & pathEnd) {
            getOrFailures {
              OrderQueries.findOne(refNum)
            }
          } ~
          pathPrefix("line-items") {
            (patch & pathEnd & entity(as[Seq[UpdateOrderLineItemsPayload]])) { reqItems ⇒
              mutateOrFailures {
                LineItemUpdater.updateOrderLineItems(auth.model, reqItems, refNum)
              }
            }
          } ~
          // depreciated in favor of /orders/line-items
          pathPrefix("order-line-items") {
            (patch & pathEnd & entity(as[Seq[UpdateOrderLineItemsPayload]])) { reqItems ⇒
              mutateOrFailures {
                LineItemUpdater.updateOrderLineItems(auth.model, reqItems, refNum)
              }
            }
          } ~
          (patch & pathEnd & entity(as[UpdateOrderPayload])) { payload ⇒
            mutateOrFailures {
              OrderStateUpdater.updateState(auth.model, refNum, payload.state)
            }
          } ~
          // depreciated in favor of /carts route
          (post & path("coupon" / Segment) & pathEnd) { code ⇒
            mutateOrFailures {
              CartPromotionUpdater.attachCoupon(auth.model, refNum.some, code)
            }
          } ~
          // depreciated in favor of /carts route
          (delete & path("coupon") & pathEnd) {
            mutateOrFailures {
              CartPromotionUpdater.detachCoupon(auth.model, refNum.some)
            }
          } ~
          (post & path("increase-remorse-period") & pathEnd) {
            mutateOrFailures {
              OrderUpdater.increaseRemorsePeriod(refNum, auth.model)
            }
          } ~
          // depreciated in favor of /carts route
          (post & path("lock") & pathEnd) {
            mutateOrFailures {
              CartLockUpdater.lock(refNum, auth.model)
            }
          } ~
          // depreciated in favor of /carts route
          (post & path("unlock") & pathEnd) {
            mutateOrFailures {
              CartLockUpdater.unlock(refNum)
            }
          } ~
          // depreciated in favor of /carts route
          (post & path("checkout")) {
            mutateOrFailures {
              Checkout.fromCart(refNum)
            }
          } ~
          // depreciated in favor of /carts route
          (post & path("line-items") & pathEnd & entity(as[Seq[UpdateLineItemsPayload]])) {
            reqItems ⇒
              mutateOrFailures {
                LineItemUpdater.updateQuantitiesOnCart(auth.model, refNum, reqItems)
              }
          } ~
          // depreciated in favor of /carts route
          (patch & path("line-items") & pathEnd & entity(as[Seq[UpdateLineItemsPayload]])) {
            reqItems ⇒
              mutateOrFailures {
                LineItemUpdater.addQuantitiesOnCart(auth.model, refNum, reqItems)
              }
          } ~
          // depreciated in favor of /carts route
          pathPrefix("payment-methods" / "credit-cards") {
            (post & pathEnd & entity(as[CreditCardPayment])) { payload ⇒
              mutateOrFailures {
                CartPaymentUpdater.addCreditCard(auth.model, payload.creditCardId, refNum.some)
              }
            } ~
            // depreciated in favor of /carts route
            (delete & pathEnd) {
              mutateOrFailures {
                CartPaymentUpdater.deleteCreditCard(auth.model, refNum.some)
              }
            }
          } ~
          pathPrefix("payment-methods" / "gift-cards") {
            // depreciated in favor of /carts route
            (post & pathEnd & entity(as[GiftCardPayment])) { payload ⇒
              mutateOrFailures {
                CartPaymentUpdater.addGiftCard(auth.model, payload, refNum.some)
              }
            } ~
            // depreciated in favor of /carts route
            (patch & pathEnd & entity(as[GiftCardPayment])) { payload ⇒
              mutateOrFailures {
                CartPaymentUpdater.editGiftCard(auth.model, payload, refNum.some)
              }
            } ~
            // depreciated in favor of /carts route
            (delete & path(GiftCard.giftCardCodeRegex) & pathEnd) { code ⇒
              mutateOrFailures {
                CartPaymentUpdater.deleteGiftCard(auth.model, code, refNum.some)
              }
            }
          } ~
          pathPrefix("payment-methods" / "store-credit") {
            // depreciated in favor of /carts route
            (post & pathEnd & entity(as[StoreCreditPayment])) { payload ⇒
              mutateOrFailures {
                CartPaymentUpdater.addStoreCredit(auth.model, payload, refNum.some)
              }
            } ~
            // depreciated in favor of /carts route
            (delete & pathEnd) {
              mutateOrFailures {
                CartPaymentUpdater.deleteStoreCredit(auth.model, refNum.some)
              }
            }
          } ~
          pathPrefix("shipping-address") {
            // depreciated in favor of /carts route
            (post & pathEnd & entity(as[CreateAddressPayload])) { payload ⇒
              mutateOrFailures {
                CartShippingAddressUpdater.createShippingAddressFromPayload(auth.model,
                                                                            payload,
                                                                            Some(refNum))
              }
            } ~
            // depreciated in favor of /carts route
            (patch & path(IntNumber) & pathEnd) { addressId ⇒
              mutateOrFailures {
                CartShippingAddressUpdater.createShippingAddressFromAddressId(auth.model,
                                                                              addressId,
                                                                              Some(refNum))
              }
            } ~
            // depreciated in favor of /carts route
            (patch & pathEnd & entity(as[UpdateAddressPayload])) { payload ⇒
              mutateOrFailures {
                CartShippingAddressUpdater.updateShippingAddressFromPayload(auth.model,
                                                                            payload,
                                                                            Some(refNum))
              }
            } ~
            // depreciated in favor of /carts route
            (delete & pathEnd) {
              mutateOrFailures {
                CartShippingAddressUpdater.removeShippingAddress(auth.model, Some(refNum))
              }
            }
          } ~
          pathPrefix("shipping-method") {
            // depreciated in favor of /carts route
            (patch & pathEnd & entity(as[UpdateShippingMethod])) { payload ⇒
              mutateOrFailures {
                CartShippingMethodUpdater.updateShippingMethod(auth.model, payload, Some(refNum))
              }
            } ~
            // depreciated in favor of /carts route
            (delete & pathEnd) {
              mutateOrFailures {
                CartShippingMethodUpdater.deleteShippingMethod(auth.model, Some(refNum))
              }
            }
          }
        }
      }
    }
  }
}
