package routes

import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.payment.giftcard.GiftCard
import models.order.Order.orderRefNumRegex
import models.inventory.Sku.skuCodeRegex
import payloads.{UpdateCustomerPayload, CreateAddressPayload, UpdateLineItemsPayload}
import services.orders.{OrderShippingAddressUpdater, OrderShippingMethodUpdater, OrderPaymentUpdater, OrderQueries}
import services.{SaveForLaterManager, StoreCreditAdjustmentsService, ShippingManager, Checkout,
CreditCardManager, AddressManager, CustomerManager, LineItemUpdater, StoreCreditService}
import models.traits.Originator
import slick.driver.PostgresDriver.api._
import utils.Apis
import utils.CustomDirectives._
import utils.Http._
import utils.Slick.implicits._

import scala.concurrent.ExecutionContext

object Customer {
  def routes(implicit ec: ExecutionContext, db: Database,
    mat: Materializer, customerAuth: AsyncAuthenticator[models.customer.Customer], apis: Apis) = {

    pathPrefix("my") {
      authenticateBasicAsync(realm = "private customer routes", customerAuth) { customer ⇒
        pathPrefix("cart") {
          (get & pathEnd) {
            activityContext(customer) { implicit ac ⇒
              goodOrFailures {
                OrderQueries.findOrCreateCartByCustomer(customer)
              }
            }
          } ~
          (post & path("line-items") & pathEnd & entity(as[Seq[UpdateLineItemsPayload]])) { reqItems ⇒
            activityContext(customer) { implicit ac ⇒
              goodOrFailures {
                LineItemUpdater.updateQuantitiesOnCustomersOrder(customer, reqItems)
              }
            }
          } ~
          (post & path("checkout") & pathEnd) {
            activityContext(customer) { implicit ac ⇒
              goodOrFailures {
                Checkout.fromCustomerCart(customer)
              }
            }
          } ~
          pathPrefix("payment-methods" / "credit-cards") {
            (post & pathEnd & entity(as[payloads.CreditCardPayment])) { payload ⇒
              activityContext(customer) { implicit ac ⇒
                goodOrFailures {
                  OrderPaymentUpdater.addCreditCard(Originator(customer), payload.creditCardId)
                }
              }
            } ~
            (delete & pathEnd) {
              activityContext(customer) { implicit ac ⇒
                goodOrFailures {
                  OrderPaymentUpdater.deleteCreditCard(Originator(customer))
                }
              }
            }
          } ~
          pathPrefix("payment-methods" / "gift-cards") {
            (post & pathEnd & entity(as[payloads.GiftCardPayment])) { payload ⇒
              activityContext(customer) { implicit ac ⇒
                goodOrFailures {
                  OrderPaymentUpdater.addGiftCard(Originator(customer), payload)
                }
              }
            } ~
            (delete & path(GiftCard.giftCardCodeRegex) & pathEnd) { code ⇒
              activityContext(customer) { implicit ac ⇒
                goodOrFailures {
                  OrderPaymentUpdater.deleteGiftCard(Originator(customer), code)
                }
              }
            }
          } ~
          pathPrefix("payment-methods" / "store-credit") {
            (post & pathEnd & entity(as[payloads.StoreCreditPayment])) { payload ⇒
              activityContext(customer) { implicit ac ⇒
                goodOrFailures {
                  OrderPaymentUpdater.addStoreCredit(Originator(customer), payload)
                }
              }
            } ~
            (delete & pathEnd) {
              activityContext(customer) { implicit ac ⇒
                goodOrFailures {
                  OrderPaymentUpdater.deleteStoreCredit(Originator(customer))
                }
              }
            }
          } ~
          pathPrefix("shipping-address") {
            (post & pathEnd & entity(as[payloads.CreateAddressPayload])) { payload ⇒
              activityContext(customer) { implicit ac ⇒
                goodOrFailures {
                  OrderShippingAddressUpdater.createShippingAddressFromPayload(Originator(customer), payload)
                }
              }
            } ~
            (patch & path(IntNumber) & pathEnd) { addressId ⇒
              activityContext(customer) { implicit ac ⇒
                goodOrFailures {
                  OrderShippingAddressUpdater.createShippingAddressFromAddressId(Originator(customer), addressId)
                }
              }
            } ~
            (patch & pathEnd & entity(as[payloads.UpdateAddressPayload])) { payload ⇒
              activityContext(customer) { implicit ac ⇒
                goodOrFailures {
                  OrderShippingAddressUpdater.updateShippingAddressFromPayload(Originator(customer), payload)
                }
              }
            } ~
            (delete & pathEnd) {
              activityContext(customer) { implicit ac ⇒
                goodOrFailures {
                  OrderShippingAddressUpdater.removeShippingAddress(Originator(customer))
                }
              }
            }
          } ~
          pathPrefix("shipping-method") {
            (patch & pathEnd & entity(as[payloads.UpdateShippingMethod])) { payload ⇒
              activityContext(customer) { implicit ac ⇒
                goodOrFailures {
                  OrderShippingMethodUpdater.updateShippingMethod(Originator(customer), payload)
                }
              }
            } ~
            (delete & pathEnd) {
              activityContext(customer) { implicit ac ⇒
                goodOrFailures {
                  OrderShippingMethodUpdater.deleteShippingMethod(Originator(customer))
                }
              }
            }
          }
        } ~
        pathPrefix("account") {
          (get & pathEnd) {
            goodOrFailures {
              CustomerManager.getById(customer.id)
            }
          } ~
          (patch & pathEnd & entity(as[UpdateCustomerPayload])) { payload ⇒
            activityContext(customer) { implicit ac ⇒
              goodOrFailures {
                CustomerManager.update(customer.id, payload)
              }
            }
          }
        } ~
        pathPrefix("orders") {
          (get & pathEnd & sortAndPage) { implicit sortAndPage ⇒
            goodOrFailures {
              OrderQueries.listByCustomer(customer)
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
        pathPrefix("shipping-methods" / orderRefNumRegex) { refNum ⇒
          (get & pathEnd) {
            goodOrFailures {
              ShippingManager.getShippingMethodsForOrder(refNum, Some(customer))
            }
          }
        } ~
        pathPrefix("addresses") {
          (get & pathEnd & sortAndPage) { implicit sortAndPage ⇒
            goodOrFailures {
              AddressManager.findAllVisibleByCustomer(customer.id)
            }
          } ~
          (post & pathEnd & entity(as[CreateAddressPayload])) { payload ⇒
            activityContext(customer) { implicit ac ⇒
              goodOrFailures {
                AddressManager.create(payload, customer.id)
              }
            }
          } ~
          (delete & path("default") & pathEnd) {
            nothingOrFailures {
              AddressManager.removeDefaultShippingAddress(customer.id)
            }
          }
        } ~
        pathPrefix("addresses" / IntNumber) { addressId ⇒
          (get & pathEnd) {
            goodOrFailures {
              AddressManager.getByIdAndCustomer(addressId, customer)
            }
          } ~
          (post & path("default") & pathEnd) {
            goodOrFailures {
              AddressManager.setDefaultShippingAddress(customer.id, addressId)
            }
          } ~
          (patch & pathEnd & entity(as[CreateAddressPayload])) { payload ⇒
            activityContext(customer) { implicit ac ⇒
              goodOrFailures {
                AddressManager.edit(addressId, customer.id, payload)
              }
            }
          } ~
          (delete & pathEnd) {
            activityContext(customer) { implicit ac ⇒
              nothingOrFailures {
                AddressManager.remove(customer.id, addressId)
              }
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
          (post & path(IntNumber / "default") & pathEnd & entity(as[payloads.ToggleDefaultCreditCard])) {
            (cardId, payload) ⇒
              goodOrFailures {
                CreditCardManager.toggleCreditCardDefault(customer.id, cardId, payload.isDefault)
              }
          } ~
          (post & pathEnd & entity(as[payloads.CreateCreditCard])) { payload ⇒
            activityContext(customer) { implicit ac ⇒
              goodOrFailures {
                CreditCardManager.createCardThroughGateway(customer.id, payload)
              }
            }
          } ~
          (patch & path(IntNumber) & pathEnd & entity(as[payloads.EditCreditCard])) { (cardId, payload) ⇒
            activityContext(customer) { implicit ac ⇒
              nothingOrFailures {
                CreditCardManager.editCreditCard(customer.id, cardId, payload)
              }
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { cardId ⇒
            activityContext(customer) { implicit ac ⇒
              nothingOrFailures {
                CreditCardManager.deleteCreditCard(customer.id, cardId)
              }
            }
          }
        } ~
        pathPrefix("payment-methods" / "store-credits") {
          (get & pathEnd & sortAndPage) { implicit sortAndPage ⇒
            goodOrFailures {
              StoreCreditService.findAllByCustomer(customer.id)
            }
          } ~
          (get & path(IntNumber) & pathEnd) { storeCreditId ⇒
            goodOrFailures {
              StoreCreditService.getByIdAndCustomer(storeCreditId, customer)
            }
          } ~
          (get & path("transactions") & pathEnd & sortAndPage) { implicit sortAndPage ⇒
            goodOrFailures {
              StoreCreditAdjustmentsService.forCustomer(customer.id)
            }
          } ~
          (get & path("totals") & pathEnd) {
            goodOrFailures {
              StoreCreditService.totalsForCustomer(customer.id)
            }
          }
        } ~
        pathPrefix("save-for-later") {
          (get & pathEnd) {
            goodOrFailures {
              SaveForLaterManager.findAll(customer.id)
            }
          } ~
          (post & path(skuCodeRegex) & pathEnd) { code ⇒
            goodOrFailures {
              SaveForLaterManager.saveForLater(customer.id, code)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { id ⇒
            nothingOrFailures {
              SaveForLaterManager.deleteSaveForLater(id)
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

