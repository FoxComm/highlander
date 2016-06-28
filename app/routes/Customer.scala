package routes

import akka.http.scaladsl.server.Directives._

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.auth.CustomerToken
import models.inventory.Sku.skuCodeRegex
import models.order.Order.orderRefNumRegex
import models.payment.giftcard.GiftCard
import models.traits.Originator
import payloads.AddressPayloads._
import payloads.CustomerPayloads.UpdateCustomerPayload
import payloads.LineItemPayloads.UpdateLineItemsPayload
import payloads.PaymentPayloads._
import payloads.UpdateShippingMethod
import services.Authenticator.{AsyncAuthenticator, requireAuth}
import services.customers.CustomerManager
import services.orders._
import services.product.ProductManager
import services.{AddressManager, Checkout, CreditCardManager, LineItemUpdater, SaveForLaterManager, ShippingManager, StoreCreditService}
import utils.aliases._
import utils.apis.Apis
import utils.db.DbResultT.Runners._
import utils.http.CustomDirectives._
import utils.http.Http._

object Customer {
  def routes(implicit ec: EC,
             es: ES,
             db: DB,
             customerAuth: AsyncAuthenticator[models.customer.Customer],
             apis: Apis) = {

    pathPrefix("my") {
      requireAuth(customerAuth) { customer ⇒
        activityContext(customer) { implicit ac ⇒
          path("info") {
            complete(CustomerToken.fromCustomer(customer))
          } ~
          pathPrefix("products" / IntNumber / "baked") { productId ⇒
            determineObjectContext(db, ec) { implicit context ⇒
              (get & pathEnd) {
                getOrFailures {
                  ProductManager.getProduct(productId)
                }
              }
            }
          } ~
          pathPrefix("cart") {
            determineObjectContext(db, ec) { context ⇒
              (get & pathEnd) {
                goodOrFailures {
                  OrderQueries.findOrCreateCartByCustomer(customer, context)
                }
              } ~
              (post & path("line-items") & pathEnd & entity(as[Seq[UpdateLineItemsPayload]])) {
                reqItems ⇒
                  goodOrFailures {
                    LineItemUpdater.updateQuantitiesOnCustomersOrder(customer, reqItems, context)
                  }
              } ~
              (post & path("coupon" / Segment) & pathEnd) { code ⇒
                goodOrFailures {
                  OrderPromotionUpdater.attachCoupon(Originator(customer), None, context, code)
                }
              } ~
              (delete & path("coupon") & pathEnd) {
                goodOrFailures {
                  OrderPromotionUpdater.detachCoupon(Originator(customer))
                }
              } ~
              (post & path("checkout") & pathEnd) {
                goodOrFailures {
                  Checkout.fromCustomerCart(customer, context)
                }
              } ~
              pathPrefix("payment-methods" / "credit-cards") {
                (post & pathEnd & entity(as[CreditCardPayment])) { payload ⇒
                  goodOrFailures {
                    OrderPaymentUpdater
                      .addCreditCard(Originator(customer), payload.creditCardId)
                      .runTxn()
                  }
                } ~
                (delete & pathEnd) {
                  goodOrFailures {
                    OrderPaymentUpdater.deleteCreditCard(Originator(customer)).runTxn()
                  }
                }
              } ~
              pathPrefix("payment-methods" / "gift-cards") {
                (post & pathEnd & entity(as[GiftCardPayment])) { payload ⇒
                  goodOrFailures {
                    OrderPaymentUpdater.addGiftCard(Originator(customer), payload).runTxn()
                  }
                } ~
                (patch & pathEnd & entity(as[GiftCardPayment])) { payload ⇒
                  goodOrFailures {
                    OrderPaymentUpdater.editGiftCard(Originator(customer), payload).runTxn()
                  }
                } ~
                (delete & path(GiftCard.giftCardCodeRegex) & pathEnd) { code ⇒
                  goodOrFailures {
                    OrderPaymentUpdater.deleteGiftCard(Originator(customer), code).runTxn()
                  }
                }
              } ~
              pathPrefix("payment-methods" / "store-credit") {
                (post & pathEnd & entity(as[StoreCreditPayment])) { payload ⇒
                  goodOrFailures {
                    OrderPaymentUpdater.addStoreCredit(Originator(customer), payload).runTxn()
                  }
                } ~
                (delete & pathEnd) {
                  goodOrFailures {
                    OrderPaymentUpdater.deleteStoreCredit(Originator(customer)).runTxn()
                  }
                }
              } ~
              pathPrefix("shipping-address") {
                (post & pathEnd & entity(as[CreateAddressPayload])) { payload ⇒
                  mutateOrFailures {
                    OrderShippingAddressUpdater
                      .createShippingAddressFromPayload(Originator(customer), payload)
                  }
                } ~
                (patch & path(IntNumber) & pathEnd) { addressId ⇒
                  mutateOrFailures {
                    OrderShippingAddressUpdater
                      .createShippingAddressFromAddressId(Originator(customer), addressId)
                  }
                } ~
                (patch & pathEnd & entity(as[UpdateAddressPayload])) { payload ⇒
                  mutateOrFailures {
                    OrderShippingAddressUpdater
                      .updateShippingAddressFromPayload(Originator(customer), payload)
                  }
                } ~
                (delete & pathEnd) {
                  deleteOrFailures {
                    OrderShippingAddressUpdater.removeShippingAddress(Originator(customer))
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
                  goodOrFailures {
                    OrderShippingMethodUpdater.updateShippingMethod(Originator(customer), payload)
                  }
                } ~
                (delete & pathEnd) {
                  goodOrFailures {
                    OrderShippingMethodUpdater.deleteShippingMethod(Originator(customer))
                  }
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
          pathPrefix("orders" / orderRefNumRegex) { refNum ⇒
            (get & pathEnd) {
              goodOrFailures {
                OrderQueries.findOneByCustomer(refNum, customer)
              }
            }
          } ~
          pathPrefix("addresses") {
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
              goodOrFailures {
                CreditCardManager.getByIdAndCustomer(creditCardId, customer)
              }
            } ~
            (post & path(IntNumber / "default") & pathEnd & entity(as[ToggleDefaultCreditCard])) {
              (cardId, payload) ⇒
                goodOrFailures {
                  CreditCardManager.toggleCreditCardDefault(customer.id, cardId, payload.isDefault)
                }
            } ~
            (post & pathEnd & entity(as[CreateCreditCard])) { payload ⇒
              goodOrFailures {
                CreditCardManager.createCardThroughGateway(customer.id, payload)
              }
            } ~
            (patch & path(IntNumber) & pathEnd & entity(as[EditCreditCard])) { (cardId, payload) ⇒
              goodOrFailures {
                CreditCardManager.editCreditCard(customer.id, cardId, payload)
              }
            } ~
            (delete & path(IntNumber) & pathEnd) { cardId ⇒
              nothingOrFailures {
                CreditCardManager.deleteCreditCard(customer.id, cardId)
              }
            }
          } ~
          pathPrefix("payment-methods" / "store-credits") {
            (get & path(IntNumber) & pathEnd) { storeCreditId ⇒
              goodOrFailures {
                StoreCreditService.getByIdAndCustomer(storeCreditId, customer)
              }
            } ~
            (get & path("totals") & pathEnd) {
              goodOrFailures {
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
