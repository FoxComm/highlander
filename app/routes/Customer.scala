package routes

import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._

import models.payment.giftcard.GiftCard
import models.order.Order.orderRefNumRegex
import models.inventory.Sku.skuCodeRegex
import models.traits.Originator
import payloads._
import services.orders.{OrderShippingAddressUpdater, OrderShippingMethodUpdater, OrderPaymentUpdater, OrderQueries}
import services.{SaveForLaterManager, StoreCreditAdjustmentsService, ShippingManager, Checkout,
CreditCardManager, AddressManager, CustomerManager, LineItemUpdater, StoreCreditService}
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
        activityContext(customer) { implicit ac ⇒
          pathPrefix("cart") {
            (get & pathEnd) {
              goodOrFailures {
                OrderQueries.findOrCreateCartByCustomer(customer)
              }
            } ~
            (post & path("line-items") & pathEnd & entity(as[Seq[UpdateLineItemsPayload]])) { reqItems ⇒
              goodOrFailures {
                LineItemUpdater.updateQuantitiesOnCustomersOrder(customer, reqItems)
              }
            } ~
            (post & path("checkout") & pathEnd) {
              goodOrFailures {
                Checkout.fromCustomerCart(customer)
              }
            } ~
            pathPrefix("payment-methods" / "credit-cards") {
              (post & pathEnd & entity(as[CreditCardPayment])) { payload ⇒
                goodOrFailures {
                  OrderPaymentUpdater.addCreditCard(Originator(customer), payload.creditCardId)
                }
              } ~
              (delete & pathEnd) {
                goodOrFailures {
                  OrderPaymentUpdater.deleteCreditCard(Originator(customer))
                }
              }
            } ~
            pathPrefix("payment-methods" / "gift-cards") {
              (post & pathEnd & entity(as[GiftCardPayment])) { payload ⇒
                goodOrFailures {
                  OrderPaymentUpdater.addGiftCard(Originator(customer), payload)
                }
              } ~
              (delete & path(GiftCard.giftCardCodeRegex) & pathEnd) { code ⇒
                goodOrFailures {
                  OrderPaymentUpdater.deleteGiftCard(Originator(customer), code)
                }
              }
            } ~
            pathPrefix("payment-methods" / "store-credit") {
              (post & pathEnd & entity(as[StoreCreditPayment])) { payload ⇒
                goodOrFailures {
                  OrderPaymentUpdater.addStoreCredit(Originator(customer), payload)
                }
              } ~
              (delete & pathEnd) {
                goodOrFailures {
                  OrderPaymentUpdater.deleteStoreCredit(Originator(customer))
                }
              }
            } ~
            pathPrefix("shipping-address") {
              (post & pathEnd & entity(as[CreateAddressPayload])) { payload ⇒
                goodOrFailures {
                  OrderShippingAddressUpdater.createShippingAddressFromPayload(Originator(customer), payload)
                }
              } ~
              (patch & path(IntNumber) & pathEnd) { addressId ⇒
                goodOrFailures {
                  OrderShippingAddressUpdater.createShippingAddressFromAddressId(Originator(customer), addressId)
                }
              } ~
              (patch & pathEnd & entity(as[UpdateAddressPayload])) { payload ⇒
                goodOrFailures {
                  OrderShippingAddressUpdater.updateShippingAddressFromPayload(Originator(customer), payload)
                }
              } ~
              (delete & pathEnd) {
                goodOrFailures {
                  OrderShippingAddressUpdater.removeShippingAddress(Originator(customer))
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
          } ~
          pathPrefix("account") {
            (get & pathEnd) {
              goodOrFailures {
                CustomerManager.getById(customer.id)
              }
            } ~
            (patch & pathEnd & entity(as[UpdateCustomerPayload])) { payload ⇒
              goodOrFailures {
                CustomerManager.update(customer.id, payload)
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
              goodOrFailures {
                AddressManager.create(payload, customer.id)
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
              goodOrFailures {
                AddressManager.edit(addressId, customer.id, payload)
              }
            } ~
            (delete & pathEnd) {
              nothingOrFailures {
                AddressManager.remove(customer.id, addressId)
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
            (post & path(IntNumber / "default") & pathEnd & entity(as[ToggleDefaultCreditCard])) { (cardId, payload) ⇒
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
              nothingOrFailures {
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
}

