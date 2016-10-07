package routes

import akka.http.scaladsl.server.Directives._

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.auth.CustomerToken
import models.cord.Cord.cordRefNumRegex
import models.inventory.Sku.skuCodeRegex
import models.payment.giftcard.GiftCard
import models.traits.Originator
import payloads.AddressPayloads._
import payloads.CustomerPayloads.UpdateCustomerPayload
import payloads.LineItemPayloads.UpdateLineItemsPayload
import payloads.PaymentPayloads._
import payloads.UpdateShippingMethod
import services.Authenticator.{AsyncAuthenticator, requireCustomerAuth}
import services._
import services.carts._
import services.customers.CustomerManager
import services.product.ProductManager
import utils.aliases._
import utils.apis.Apis
import utils.http.CustomDirectives._
import utils.http.Http._

object Customer {
  def routes(implicit ec: EC,
             es: ES,
             db: DB,
             customerAuth: AsyncAuthenticator[models.customer.Customer],
             apis: Apis) = {

    pathPrefix("my") {
      requireCustomerAuth(customerAuth) { customer ⇒
        activityContext(customer) { implicit ac ⇒
          determineObjectContext(db, ec) { implicit ctx ⇒
            path("info") {
              complete(CustomerToken.fromCustomer(customer))
            } ~
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
                  CartQueries.findOrCreateCartByCustomer(customer, ctx)
                }
              } ~
              (post & path("line-items") & pathEnd & entity(as[Seq[UpdateLineItemsPayload]])) {
                reqItems ⇒
                  mutateOrFailures {
                    LineItemUpdater.updateLineItemsOnCustomersCart(customer, reqItems)
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
                  CartPromotionUpdater.attachCoupon(Originator(customer), None, code)
                }
              } ~
              (delete & path("coupon") & pathEnd) {
                mutateOrFailures {
                  CartPromotionUpdater.detachCoupon(Originator(customer))
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
                    CartPaymentUpdater.addCreditCard(Originator(customer), payload.creditCardId)
                  }
                } ~
                (delete & pathEnd) {
                  mutateOrFailures {
                    CartPaymentUpdater.deleteCreditCard(Originator(customer))
                  }
                }
              } ~
              pathPrefix("payment-methods" / "gift-cards") {
                (post & pathEnd & entity(as[GiftCardPayment])) { payload ⇒
                  mutateOrFailures {
                    CartPaymentUpdater.addGiftCard(Originator(customer), payload)
                  }
                } ~
                (patch & pathEnd & entity(as[GiftCardPayment])) { payload ⇒
                  mutateOrFailures {
                    CartPaymentUpdater.editGiftCard(Originator(customer), payload)
                  }
                } ~
                (delete & path(GiftCard.giftCardCodeRegex) & pathEnd) { code ⇒
                  mutateOrFailures {
                    CartPaymentUpdater.deleteGiftCard(Originator(customer), code)
                  }
                }
              } ~
              pathPrefix("payment-methods" / "store-credit") {
                (post & pathEnd & entity(as[StoreCreditPayment])) { payload ⇒
                  mutateOrFailures {
                    CartPaymentUpdater.addStoreCredit(Originator(customer), payload)
                  }
                } ~
                (delete & pathEnd) {
                  mutateOrFailures {
                    CartPaymentUpdater.deleteStoreCredit(Originator(customer))
                  }
                }
              } ~
              pathPrefix("shipping-address") {
                (post & pathEnd & entity(as[CreateAddressPayload])) { payload ⇒
                  mutateOrFailures {
                    CartShippingAddressUpdater
                      .createShippingAddressFromPayload(Originator(customer), payload)
                  }
                } ~
                (patch & path(IntNumber) & pathEnd) { addressId ⇒
                  mutateOrFailures {
                    CartShippingAddressUpdater
                      .createShippingAddressFromAddressId(Originator(customer), addressId)
                  }
                } ~
                (patch & pathEnd & entity(as[UpdateAddressPayload])) { payload ⇒
                  mutateOrFailures {
                    CartShippingAddressUpdater
                      .updateShippingAddressFromPayload(Originator(customer), payload)
                  }
                } ~
                (delete & pathEnd) {
                  deleteOrFailures {
                    CartShippingAddressUpdater.removeShippingAddress(Originator(customer))
                  }
                }
              } ~
              pathPrefix("shipping-methods") {
                (get & pathEnd) {
                  getOrFailures {
                    ShippingManager.getShippingMethodsForCart(Originator(customer))
                  }
                }
              } ~
              pathPrefix("shipping-method") {
                (patch & pathEnd & entity(as[UpdateShippingMethod])) { payload ⇒
                  mutateOrFailures {
                    CartShippingMethodUpdater.updateShippingMethod(Originator(customer), payload)
                  }
                } ~
                (delete & pathEnd) {
                  mutateOrFailures {
                    CartShippingMethodUpdater.deleteShippingMethod(Originator(customer))
                  }
                }
              }
            } ~
            pathPrefix("account") {
              (get & pathEnd) {
                getOrFailures {
                  CustomerManager.getById(customer.id)
                }
              } ~
              (patch & pathEnd & entity(as[UpdateCustomerPayload])) { payload ⇒
                mutateOrFailures {
                  CustomerManager.update(customer.id, payload)
                }
              }
            } ~
            pathPrefix("orders" / cordRefNumRegex) { refNum ⇒
              (get & pathEnd) {
                getOrFailures {
                  CartQueries.findOneByCustomer(refNum, customer)
                }
              }
            } ~
            pathPrefix("addresses") {
              (get & pathEnd) {
                getOrFailures {
                  AddressManager.findAllByCustomer(customer.id)
                }
              } ~
              (post & pathEnd & entity(as[CreateAddressPayload])) { payload ⇒
                mutateOrFailures {
                  AddressManager.create(Originator(customer), payload, customer.id)
                }
              } ~
              (delete & path("default") & pathEnd) {
                deleteOrFailures {
                  AddressManager.removeDefaultShippingAddress(customer.id)
                }
              }
            } ~
            pathPrefix("addresses" / IntNumber) { addressId ⇒
              (get & pathEnd) {
                getOrFailures {
                  AddressManager.get(Originator(customer), addressId, customer.id)
                }
              } ~
              (post & path("default") & pathEnd) {
                mutateOrFailures {
                  AddressManager.setDefaultShippingAddress(addressId, customer.id)
                }
              } ~
              (patch & pathEnd & entity(as[CreateAddressPayload])) { payload ⇒
                mutateOrFailures {
                  AddressManager.edit(Originator(customer), addressId, customer.id, payload)
                }
              } ~
              (delete & pathEnd) {
                deleteOrFailures {
                  AddressManager.remove(Originator(customer), addressId, customer.id)
                }
              }
            } ~
            pathPrefix("payment-methods" / "credit-cards") {
              (get & pathEnd) {
                complete {
                  CreditCardManager.creditCardsInWalletFor(customer.id)
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
                    CreditCardManager.toggleCreditCardDefault(customer.id,
                                                              cardId,
                                                              payload.isDefault)
                  }
              } ~
              (post & pathEnd & entity(as[CreateCreditCardFromTokenPayload])) { payload ⇒
                mutateOrFailures {
                  CreditCardManager.createCardFromToken(customer.id, payload)
                }
              } ~
              (patch & path(IntNumber) & pathEnd & entity(as[EditCreditCard])) {
                (cardId, payload) ⇒
                  mutateOrFailures {
                    CreditCardManager.editCreditCard(customer.id, cardId, payload)
                  }
              } ~
              (delete & path(IntNumber) & pathEnd) { cardId ⇒
                deleteOrFailures {
                  CreditCardManager.deleteCreditCard(customer.id, cardId)
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
                  StoreCreditService.totalsForCustomer(customer.id)
                }
              }
            } ~
            pathPrefix("save-for-later") {
              determineObjectContext(db, ec) { context ⇒
                (get & pathEnd) {
                  getOrFailures {
                    SaveForLaterManager.findAll(customer.id, context.id)
                  }
                } ~
                (post & path(skuCodeRegex) & pathEnd) { code ⇒
                  mutateOrFailures {
                    SaveForLaterManager.saveForLater(customer.id, code, context)
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
