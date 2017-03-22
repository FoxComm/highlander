package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cats.implicits._
import utils.http.JsonSupport._
import models.account.User
import models.cord.Cord.cordRefNumRegex
import models.payment.giftcard.GiftCard
import payloads.AddressPayloads._
import payloads.CartPayloads._
import payloads.LineItemPayloads._
import payloads.PaymentPayloads._
import payloads.UpdateShippingMethod
import services.Authenticator.AuthData
import services.carts._
import services.{Checkout, LineItemUpdater}
import utils.aliases._
import utils.apis.Apis
import utils.http.CustomDirectives._
import utils.http.Http._

object CartRoutes {

  def routes(implicit ec: EC, es: ES, db: DB, auth: AuthData[User], apis: Apis): Route = {

    activityContext(auth) { implicit ac ⇒
      determineObjectContext(db, ec) { implicit ctx ⇒
        pathPrefix("carts") {
          (post & pathEnd & entity(as[CreateCart])) { payload ⇒
            mutateOrFailures {
              CartCreator.createCart(auth.model, payload)
            }
          } ~
          pathPrefix(cordRefNumRegex) { refNum ⇒
            (get & pathEnd) {
              getOrFailures {
                CartQueries.findOne(refNum)
              }
            } ~
            pathPrefix("coupon") {
              (post & path(Segment) & pathEnd) { code ⇒
                mutateOrFailures {
                  CartPromotionUpdater.attachCoupon(auth.model, refNum.some, code)
                }
              } ~
              (delete & pathEnd) {
                mutateOrFailures {
                  CartPromotionUpdater.detachCoupon(auth.model, refNum.some)
                }
              }
            } ~
            (post & path("checkout")) {
              mutateOrFailures {
                Checkout.fromCart(refNum)
              }
            } ~
            pathPrefix("line-items") {
              (post & pathEnd & entity(as[Seq[UpdateLineItemsPayload]])) { reqItems ⇒
                mutateOrFailures {
                  LineItemUpdater.updateQuantitiesOnCart(auth.model, refNum, reqItems)
                }
              } ~
              (patch & pathEnd & entity(as[Seq[UpdateLineItemsPayload]])) { reqItems ⇒
                mutateOrFailures {
                  LineItemUpdater.addQuantitiesOnCart(auth.model, refNum, reqItems)
                }
              } ~
              (patch & path("attributes") & pathEnd & entity(as[Seq[UpdateOrderLineItemsPayload]])) {
                reqItems ⇒
                  mutateOrFailures {
                    LineItemUpdater.updateOrderLineItems(auth.model, reqItems, refNum)
                  }
              }
            } ~
            pathPrefix("payment-methods") {
              pathPrefix("credit-cards") {
                (post & pathEnd & entity(as[CreditCardPayment])) { payload ⇒
                  mutateOrFailures {
                    CartPaymentUpdater.addCreditCard(auth.model, payload.creditCardId, refNum.some)
                  }
                } ~
                (delete & pathEnd) {
                  mutateOrFailures {
                    CartPaymentUpdater.deleteCreditCard(auth.model, refNum.some)
                  }
                }
              } ~
              pathPrefix("gift-cards") {
                (post & pathEnd & entity(as[GiftCardPayment])) { payload ⇒
                  mutateOrFailures {
                    CartPaymentUpdater.addGiftCard(auth.model, payload, refNum.some)
                  }
                } ~
                (patch & pathEnd & entity(as[GiftCardPayment])) { payload ⇒
                  mutateOrFailures {
                    CartPaymentUpdater.editGiftCard(auth.model, payload, refNum.some)
                  }
                } ~
                (delete & path(GiftCard.giftCardCodeRegex) & pathEnd) { code ⇒
                  mutateOrFailures {
                    CartPaymentUpdater.deleteGiftCard(auth.model, code, refNum.some)
                  }
                }
              } ~
              pathPrefix("store-credit") {
                (post & pathEnd & entity(as[StoreCreditPayment])) { payload ⇒
                  mutateOrFailures {
                    CartPaymentUpdater.addStoreCredit(auth.model, payload, refNum.some)
                  }
                } ~
                (delete & pathEnd) {
                  mutateOrFailures {
                    CartPaymentUpdater.deleteStoreCredit(auth.model, refNum.some)
                  }
                }
              }
            } ~
            pathPrefix("shipping-address") {
              (post & pathEnd & entity(as[CreateAddressPayload])) { payload ⇒
                mutateOrFailures {
                  CartShippingAddressUpdater.createShippingAddressFromPayload(auth.model,
                                                                              payload,
                                                                              Some(refNum))
                }
              } ~
              (patch & path(IntNumber) & pathEnd) { addressId ⇒
                mutateOrFailures {
                  CartShippingAddressUpdater.createShippingAddressFromAddressId(auth.model,
                                                                                addressId,
                                                                                Some(refNum))
                }
              } ~
              (patch & pathEnd & entity(as[UpdateAddressPayload])) { payload ⇒
                mutateOrFailures {
                  CartShippingAddressUpdater.updateShippingAddressFromPayload(auth.model,
                                                                              payload,
                                                                              Some(refNum))
                }
              } ~
              (delete & pathEnd) {
                mutateOrFailures {
                  CartShippingAddressUpdater.removeShippingAddress(auth.model, Some(refNum))
                }
              }
            } ~
            pathPrefix("shipping-method") {
              (patch & pathEnd & entity(as[UpdateShippingMethod])) { payload ⇒
                mutateOrFailures {
                  CartShippingMethodUpdater.updateShippingMethod(auth.model, payload, Some(refNum))
                }
              } ~
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
}
