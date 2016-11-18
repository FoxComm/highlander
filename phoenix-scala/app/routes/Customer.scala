package routes

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse}
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._

import models.cord.Cord.cordRefNumRegex
import models.inventory.Sku.skuCodeRegex
import models.payment.giftcard.GiftCard
import payloads.AddressPayloads._
import payloads.CustomerPayloads._
import payloads.LineItemPayloads.UpdateLineItemsPayload
import payloads.PaymentPayloads._
import payloads.UpdateShippingMethod
import services.Authenticator.{UserAuthenticator, requireCustomerAuth}
import services._
import services.carts._
import services.customers.CustomerManager
import services.product.ProductManager
import utils.aliases._
import utils.apis.Apis
import utils.http.CustomDirectives._
import org.json4s.jackson.Serialization.{write ⇒ json}
import utils.db._
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import services.orders.OrderQueries
import utils.http.Http._

object Customer {
  def routes(implicit ec: EC, es: ES, db: DB, auth: UserAuthenticator, apis: Apis) = {

    pathPrefix("my") {
      requireCustomerAuth(auth) { implicit auth ⇒
        activityContext(auth.model) { implicit ac ⇒
          determineObjectContext(db, ec) { implicit ctx ⇒
            pathPrefix("products" / IntNumber / "baked") { productId ⇒
              (get & pathEnd) {
                getOrFailures {
                  ProductManager.getProduct(productId)
                }
              }
            } ~
            pathPrefix("cart") {
              (get & pathEnd) {
                getOrFailures {
                  CartQueries.findOrCreateCartByAccountId(auth.account.id, ctx)
                }
              } ~
              (post & path("line-items") & pathEnd & entity(as[Seq[UpdateLineItemsPayload]])) {
                reqItems ⇒
                  mutateOrFailures {
                    LineItemUpdater.updateQuantitiesOnCustomersCart(auth.model, reqItems)
                  }
              } ~
              (patch & path("line-items") & pathEnd & entity(as[Seq[UpdateLineItemsPayload]])) {
                reqItems ⇒
                  mutateOrFailures {
                    LineItemUpdater.addQuantitiesOnCustomersCart(auth.model, reqItems)
                  }
              } ~
              (post & path("coupon" / Segment) & pathEnd) { code ⇒
                mutateOrFailures {
                  CartPromotionUpdater.attachCoupon(auth.model, None, code)
                }
              } ~
              (delete & path("coupon") & pathEnd) {
                mutateOrFailures {
                  CartPromotionUpdater.detachCoupon(auth.model)
                }
              } ~
              (post & path("checkout") & pathEnd) {
                mutateOrFailures {
                  Checkout.forCustomer(auth.model)
                }
              } ~
              pathPrefix("payment-methods" / "credit-cards") {
                (post & pathEnd & entity(as[CreditCardPayment])) { payload ⇒
                  mutateOrFailures {
                    CartPaymentUpdater.addCreditCard(auth.model, payload.creditCardId)
                  }
                } ~
                (delete & pathEnd) {
                  mutateOrFailures {
                    CartPaymentUpdater.deleteCreditCard(auth.model)
                  }
                }
              } ~
              pathPrefix("payment-methods" / "gift-cards") {
                (post & pathEnd & entity(as[GiftCardPayment])) { payload ⇒
                  mutateOrFailures {
                    CartPaymentUpdater.addGiftCard(auth.model, payload)
                  }
                } ~
                (patch & pathEnd & entity(as[GiftCardPayment])) { payload ⇒
                  mutateOrFailures {
                    CartPaymentUpdater.editGiftCard(auth.model, payload)
                  }
                } ~
                (delete & path(GiftCard.giftCardCodeRegex) & pathEnd) { code ⇒
                  mutateOrFailures {
                    CartPaymentUpdater.deleteGiftCard(auth.model, code)
                  }
                }
              } ~
              pathPrefix("payment-methods" / "store-credit") {
                (post & pathEnd & entity(as[StoreCreditPayment])) { payload ⇒
                  mutateOrFailures {
                    CartPaymentUpdater.addStoreCredit(auth.model, payload)
                  }
                } ~
                (delete & pathEnd) {
                  mutateOrFailures {
                    CartPaymentUpdater.deleteStoreCredit(auth.model)
                  }
                }
              } ~
              pathPrefix("shipping-address") {
                (post & pathEnd & entity(as[CreateAddressPayload])) { payload ⇒
                  mutateOrFailures {
                    CartShippingAddressUpdater.createShippingAddressFromPayload(auth.model,
                                                                                payload)
                  }
                } ~
                (patch & path(IntNumber) & pathEnd) { addressId ⇒
                  mutateOrFailures {
                    CartShippingAddressUpdater.createShippingAddressFromAddressId(auth.model,
                                                                                  addressId)
                  }
                } ~
                (patch & pathEnd & entity(as[UpdateAddressPayload])) { payload ⇒
                  mutateOrFailures {
                    CartShippingAddressUpdater.updateShippingAddressFromPayload(auth.model,
                                                                                payload)
                  }
                } ~
                (delete & pathEnd) {
                  deleteOrFailures {
                    CartShippingAddressUpdater.removeShippingAddress(auth.model)
                  }
                }
              } ~
              pathPrefix("shipping-methods") {
                (get & pathEnd) {
                  getOrFailures {
                    ShippingManager.getShippingMethodsForCart(auth.model)
                  }
                }
              } ~
              pathPrefix("shipping-method") {
                (patch & pathEnd & entity(as[UpdateShippingMethod])) { payload ⇒
                  mutateOrFailures {
                    CartShippingMethodUpdater.updateShippingMethod(auth.model, payload)
                  }
                } ~
                (delete & pathEnd) {
                  mutateOrFailures {
                    CartShippingMethodUpdater.deleteShippingMethod(auth.model)
                  }
                }
              }
            } ~
            pathPrefix("account") {
              (get & pathEnd) {
                getOrFailures {
                  CustomerManager.getByAccountId(auth.account.id)
                }
              } ~
              (pathPrefix("change-password") & pathEnd & post & entity(
                      as[ChangeCustomerPasswordPayload])) { payload ⇒
                doOrFailures {
                  CustomerManager.changePassword(auth.account.id, payload)
                }
              } ~
              (patch & pathEnd & entity(as[UpdateCustomerPayload])) { payload ⇒
                mutateWithNewTokenOrFailures {
                  CustomerManager.update(auth.account.id, payload)
                }
              }
            } ~
            pathPrefix("orders") {
              (get & pathEnd) {
                getOrFailures {
                  OrderQueries.findAllByUser(auth.model)
                }
              } ~
              (get & pathPrefix(cordRefNumRegex) & pathEnd) { refNum ⇒
                (get & pathEnd) {
                  getOrFailures {
                    OrderQueries.findOneByUser(refNum, auth.model)
                  }
                }
              }
            } ~
            pathPrefix("addresses") {
              (get & pathEnd) {
                getOrFailures {
                  AddressManager.findAllByAccountId(auth.account.id)
                }
              } ~
              (post & pathEnd & entity(as[CreateAddressPayload])) { payload ⇒
                mutateOrFailures {
                  AddressManager.create(auth.model, payload, auth.account.id)
                }
              } ~
              (delete & path("default") & pathEnd) {
                deleteOrFailures {
                  AddressManager.removeDefaultShippingAddress(auth.account.id)
                }
              }
            } ~
            pathPrefix("addresses" / IntNumber) { addressId ⇒
              (get & pathEnd) {
                getOrFailures {
                  AddressManager.get(auth.model, addressId, auth.account.id)
                }
              } ~
              (post & path("default") & pathEnd) {
                mutateOrFailures {
                  AddressManager.setDefaultShippingAddress(addressId, auth.account.id)
                }
              } ~
              (patch & pathEnd & entity(as[CreateAddressPayload])) { payload ⇒
                mutateOrFailures {
                  AddressManager.edit(auth.model, addressId, auth.account.id, payload)
                }
              } ~
              (delete & pathEnd) {
                deleteOrFailures {
                  AddressManager.remove(auth.model, addressId, auth.account.id)
                }
              }
            } ~
            pathPrefix("payment-methods" / "credit-cards") {
              (get & pathEnd) {
                complete {
                  CreditCardManager.creditCardsInWalletFor(auth.account.id)
                }
              } ~
              (get & path(IntNumber) & pathEnd) { creditCardId ⇒
                getOrFailures {
                  CreditCardManager.getByIdAndCustomer(creditCardId, auth.model)
                }
              } ~
              (post & path(IntNumber / "default") & pathEnd & entity(as[ToggleDefaultCreditCard])) {
                (cardId, payload) ⇒
                  mutateOrFailures {
                    CreditCardManager.toggleCreditCardDefault(auth.account.id,
                                                              cardId,
                                                              payload.isDefault)
                  }
              } ~
              (post & pathEnd & entity(as[CreateCreditCardFromTokenPayload])) { payload ⇒
                mutateOrFailures {
                  CreditCardManager.createCardFromToken(auth.account.id, payload)
                }
              } ~
              (patch & path(IntNumber) & pathEnd & entity(as[EditCreditCard])) {
                (cardId, payload) ⇒
                  mutateOrFailures {
                    CreditCardManager.editCreditCard(auth.account.id, cardId, payload)
                  }
              } ~
              (delete & path(IntNumber) & pathEnd) { cardId ⇒
                deleteOrFailures {
                  CreditCardManager.deleteCreditCard(auth.account.id, cardId)
                }
              }
            } ~
            pathPrefix("payment-methods" / "store-credits") {
              (get & path(IntNumber) & pathEnd) { storeCreditId ⇒
                getOrFailures {
                  StoreCreditService.getByIdAndCustomer(storeCreditId, auth.model)
                }
              } ~
              (get & path("totals") & pathEnd) {
                getOrFailures {
                  StoreCreditService.totalsForCustomer(auth.account.id)
                }
              }
            } ~
            pathPrefix("save-for-later") {
              determineObjectContext(db, ec) { context ⇒
                (get & pathEnd) {
                  getOrFailures {
                    SaveForLaterManager.findAll(auth.account.id, context.id)
                  }
                } ~
                (post & path(skuCodeRegex) & pathEnd) { code ⇒
                  mutateOrFailures {
                    SaveForLaterManager.saveForLater(auth.account.id, code, context)
                  }
                } ~
                (delete & path(IntNumber) & pathEnd) { id ⇒
                  deleteOrFailures {
                    SaveForLaterManager.deleteSaveForLater(id)
                  }
                }
              }
            } ~
            complete {
              notFoundResponse
            }
          }
        }
      }
    }
  }
}
