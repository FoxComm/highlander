package routes

import akka.http.scaladsl.server.Directives._

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.account._
import models.auth.UserToken
import models.cord.Cord.cordRefNumRegex
import models.inventory.Sku.skuCodeRegex
import models.payment.giftcard.GiftCard
import payloads.AddressPayloads._
import payloads.CustomerPayloads.UpdateCustomerPayload
import payloads.LineItemPayloads.UpdateLineItemsPayload
import payloads.PaymentPayloads._
import payloads.UpdateShippingMethod
import services.Authenticator.{UserAuthenticator, requireCustomerAuth}
import services._
import services.account._
import services.carts._
import services.customers.CustomerManager
import services.product.ProductManager
import utils.aliases._
import utils.apis.Apis
import utils.http.CustomDirectives._
import utils.http.Http._

object Customer {
  def routes(implicit ec: EC, es: ES, db: DB, auth: UserAuthenticator, apis: Apis) = {

    pathPrefix("my") {
      requireCustomerAuth(auth) { customer ⇒
        activityContext(customer) { implicit ac ⇒
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
                  CartQueries.findOrCreateCartByAccountId(customer.accountId, ctx)
                }
              } ~
              (post & path("line-items") & pathEnd & entity(as[Seq[UpdateLineItemsPayload]])) {
                reqItems ⇒
                  mutateOrFailures {
                    LineItemUpdater.updateQuantitiesOnCustomersCart(customer, reqItems)
                  }
              } ~
              (patch & path("line-items") & pathEnd & entity(as[Seq[UpdateLineItemsPayload]])) {
                reqItems ⇒
                  mutateOrFailures {
                    LineItemUpdater.addQuantitiesOnCustomersCart(customer, reqItems)
                  }
              } ~
              (post & path("coupon" / Segment) & pathEnd) { code ⇒
                mutateOrFailures {
                  CartPromotionUpdater.attachCoupon(customer, None, code)
                }
              } ~
              (delete & path("coupon") & pathEnd) {
                mutateOrFailures {
                  CartPromotionUpdater.detachCoupon(customer)
                }
              } ~
              (post & path("checkout") & pathEnd) {
                mutateOrFailures {
                  Checkout.forCustomer(customer)
                }
              } ~
              pathPrefix("payment-methods" / "credit-cards") {
                (post & pathEnd & entity(as[CreditCardPayment])) { payload ⇒
                  mutateOrFailures {
                    CartPaymentUpdater.addCreditCard(customer, payload.creditCardId)
                  }
                } ~
                (delete & pathEnd) {
                  mutateOrFailures {
                    CartPaymentUpdater.deleteCreditCard(customer)
                  }
                }
              } ~
              pathPrefix("payment-methods" / "gift-cards") {
                (post & pathEnd & entity(as[GiftCardPayment])) { payload ⇒
                  mutateOrFailures {
                    CartPaymentUpdater.addGiftCard(customer, payload)
                  }
                } ~
                (patch & pathEnd & entity(as[GiftCardPayment])) { payload ⇒
                  mutateOrFailures {
                    CartPaymentUpdater.editGiftCard(customer, payload)
                  }
                } ~
                (delete & path(GiftCard.giftCardCodeRegex) & pathEnd) { code ⇒
                  mutateOrFailures {
                    CartPaymentUpdater.deleteGiftCard(customer, code)
                  }
                }
              } ~
              pathPrefix("payment-methods" / "store-credit") {
                (post & pathEnd & entity(as[StoreCreditPayment])) { payload ⇒
                  mutateOrFailures {
                    CartPaymentUpdater.addStoreCredit(customer, payload)
                  }
                } ~
                (delete & pathEnd) {
                  mutateOrFailures {
                    CartPaymentUpdater.deleteStoreCredit(customer)
                  }
                }
              } ~
              pathPrefix("shipping-address") {
                (post & pathEnd & entity(as[CreateAddressPayload])) { payload ⇒
                  mutateOrFailures {
                    CartShippingAddressUpdater.createShippingAddressFromPayload(customer, payload)
                  }
                } ~
                (patch & path(IntNumber) & pathEnd) { addressId ⇒
                  mutateOrFailures {
                    CartShippingAddressUpdater.createShippingAddressFromAddressId(customer,
                                                                                  addressId)
                  }
                } ~
                (patch & pathEnd & entity(as[UpdateAddressPayload])) { payload ⇒
                  mutateOrFailures {
                    CartShippingAddressUpdater.updateShippingAddressFromPayload(customer, payload)
                  }
                } ~
                (delete & pathEnd) {
                  deleteOrFailures {
                    CartShippingAddressUpdater.removeShippingAddress(customer)
                  }
                }
              } ~
              pathPrefix("shipping-methods") {
                (get & pathEnd) {
                  getOrFailures {
                    ShippingManager.getShippingMethodsForCart(customer)
                  }
                }
              } ~
              pathPrefix("shipping-method") {
                (patch & pathEnd & entity(as[UpdateShippingMethod])) { payload ⇒
                  mutateOrFailures {
                    CartShippingMethodUpdater.updateShippingMethod(customer, payload)
                  }
                } ~
                (delete & pathEnd) {
                  mutateOrFailures {
                    CartShippingMethodUpdater.deleteShippingMethod(customer)
                  }
                }
              }
            } ~
            pathPrefix("account") {
              (get & pathEnd) {
                getOrFailures {
                  CustomerManager.getByAccountId(customer.accountId)
                }
              } ~
              (patch & pathEnd & entity(as[UpdateCustomerPayload])) { payload ⇒
                mutateOrFailures {
                  CustomerManager.update(customer.accountId, payload)
                }
              }
            } ~
            pathPrefix("orders" / cordRefNumRegex) { refNum ⇒
              (get & pathEnd) {
                getOrFailures {
                  CartQueries.findOneByUser(refNum, customer)
                }
              }
            } ~
            pathPrefix("addresses") {
              (get & pathEnd) {
                getOrFailures {
                  AddressManager.findAllByAccountId(customer.accountId)
                }
              } ~
              (post & pathEnd & entity(as[CreateAddressPayload])) { payload ⇒
                mutateOrFailures {
                  AddressManager.create(customer, payload, customer.accountId)
                }
              } ~
              (delete & path("default") & pathEnd) {
                deleteOrFailures {
                  AddressManager.removeDefaultShippingAddress(customer.accountId)
                }
              }
            } ~
            pathPrefix("addresses" / IntNumber) { addressId ⇒
              (get & pathEnd) {
                getOrFailures {
                  AddressManager.get(customer, addressId, customer.accountId)
                }
              } ~
              (post & path("default") & pathEnd) {
                mutateOrFailures {
                  AddressManager.setDefaultShippingAddress(addressId, customer.accountId)
                }
              } ~
              (patch & pathEnd & entity(as[CreateAddressPayload])) { payload ⇒
                mutateOrFailures {
                  AddressManager.edit(customer, addressId, customer.accountId, payload)
                }
              } ~
              (delete & pathEnd) {
                deleteOrFailures {
                  AddressManager.remove(customer, addressId, customer.accountId)
                }
              }
            } ~
            pathPrefix("payment-methods" / "credit-cards") {
              (get & pathEnd) {
                complete {
                  CreditCardManager.creditCardsInWalletFor(customer.accountId)
                }
              } ~
              (get & path(IntNumber) & pathEnd) { creditCardId ⇒
                getOrFailures {
                  CreditCardManager.getByIdAndCustomer(creditCardId, customer)
                }
              } ~
              (post & path(IntNumber / "default") & pathEnd & entity(as[ToggleDefaultCreditCard])) {
                (cardId, payload) ⇒
                  mutateOrFailures {
                    CreditCardManager.toggleCreditCardDefault(customer.accountId,
                                                              cardId,
                                                              payload.isDefault)
                  }
              } ~
              (post & pathEnd & entity(as[CreateCreditCardFromTokenPayload])) { payload ⇒
                mutateOrFailures {
                  CreditCardManager.createCardFromToken(customer.accountId, payload)
                }
              } ~
              (patch & path(IntNumber) & pathEnd & entity(as[EditCreditCard])) {
                (cardId, payload) ⇒
                  mutateOrFailures {
                    CreditCardManager.editCreditCard(customer.accountId, cardId, payload)
                  }
              } ~
              (delete & path(IntNumber) & pathEnd) { cardId ⇒
                deleteOrFailures {
                  CreditCardManager.deleteCreditCard(customer.accountId, cardId)
                }
              }
            } ~
            pathPrefix("payment-methods" / "store-credits") {
              (get & path(IntNumber) & pathEnd) { storeCreditId ⇒
                getOrFailures {
                  StoreCreditService.getByIdAndCustomer(storeCreditId, customer)
                }
              } ~
              (get & path("totals") & pathEnd) {
                getOrFailures {
                  StoreCreditService.totalsForCustomer(customer.accountId)
                }
              }
            } ~
            pathPrefix("save-for-later") {
              determineObjectContext(db, ec) { context ⇒
                (get & pathEnd) {
                  getOrFailures {
                    SaveForLaterManager.findAll(customer.accountId, context.id)
                  }
                } ~
                (post & path(skuCodeRegex) & pathEnd) { code ⇒
                  mutateOrFailures {
                    SaveForLaterManager.saveForLater(customer.accountId, code, context)
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
