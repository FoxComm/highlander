package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.StoreAdmin
import models.customer.Customers
import models.traits.Originator
import payloads._
import services.customers._
import services.{AddressManager, CreditCardManager, CustomerCreditConverter, StoreCreditAdjustmentsService,
StoreCreditService}
import services.Authenticator.{AsyncAuthenticator}
import services.orders.OrderQueries
import slick.driver.PostgresDriver.api._
import utils.Apis
import utils.CustomDirectives._
import utils.Http._
import utils.Slick.implicits._
import utils.aliases._

import models.auth.Session.requireAdminAuth

object CustomerRoutes {

  def routes(implicit ec: EC, db: DB, mat: Materializer, storeAdminAuth: AsyncAuthenticator[StoreAdmin], apis: Apis) = {

    requireAdminAuth { admin ⇒
      activityContext(admin) { implicit ac ⇒
        pathPrefix("customers") {
          (get & pathEnd & sortAndPage) { implicit sortAndPage ⇒
            goodOrFailures {
              CustomerManager.findAll
            }
          } ~
          (get & pathPrefix("searchForNewOrder") & pathEnd & parameters('term).as(payloads.CustomerSearchForNewOrder)) { p ⇒
            sortAndPage { implicit sortAndPage ⇒
              goodOrFailures {
                CustomerManager.searchForNewOrder(p)
              }
            }
          } ~
          (post & pathEnd & entity(as[payloads.CreateCustomerPayload])) { payload ⇒
            goodOrFailures {
              CustomerManager.create(payload, Some(admin))
            }
          } ~
          pathPrefix("assignees") {
            (post & pathEnd & sortAndPage) { implicit sortAndPage ⇒
              entity(as[CustomerBulkAssignmentPayload]) { payload ⇒
                goodOrFailures {
                  CustomerAssignmentUpdater.assignBulk(admin, payload)
                }
              }
            } ~
            (post & path("delete") & pathEnd & sortAndPage) { implicit sortAndPage ⇒
              entity(as[CustomerBulkAssignmentPayload]) { payload ⇒
                goodOrFailures {
                  CustomerAssignmentUpdater.unassignBulk(admin, payload)
                }
              }
            }
          } ~
          pathPrefix("watchers") {
            (post & pathEnd & sortAndPage) { implicit sortAndPage ⇒
              entity(as[CustomerBulkWatchersPayload]) { payload ⇒
                goodOrFailures {
                  CustomerWatcherUpdater.watchBulk(admin, payload)
                }
              }
            } ~
            (post & path("delete") & pathEnd & sortAndPage) { implicit sortAndPage ⇒
              entity(as[CustomerBulkWatchersPayload]) { payload ⇒
                goodOrFailures {
                  CustomerWatcherUpdater.unwatchBulk(admin, payload)
                }
              }
            }
          }
        } ~
        pathPrefix("customers" / IntNumber) { customerId ⇒
          (get & pathEnd) {
            goodOrFailures {
              CustomerManager.getById(customerId)
            }
          } ~
          (get & path("cart")) {
            determineProductContext(db, ec) { productContext ⇒ 
              goodOrFailures {
                OrderQueries.findOrCreateCartByCustomerId(customerId, productContext, Some(admin))
              }
            }
          } ~
          (patch & pathEnd & entity(as[UpdateCustomerPayload])) { payload ⇒
            goodOrFailures {
              CustomerManager.update(customerId, payload, Some(admin))
            }
          } ~
          (post & path("activate") & pathEnd & entity(as[ActivateCustomerPayload])) { payload ⇒
            goodOrFailures {
              CustomerManager.activate(customerId, payload, admin)
            }
          } ~
          (post & path("disable") & pathEnd & entity(as[payloads.ToggleCustomerDisabled])) { payload ⇒
            goodOrFailures {
              CustomerManager.toggleDisabled(customerId, payload.disabled, admin)
            }
          } ~
          (post & path("blacklist") & pathEnd & entity(as[payloads.ToggleCustomerBlacklisted])) { payload ⇒
            goodOrFailures {
              CustomerManager.toggleBlacklisted(customerId, payload.blacklisted, admin)
            }
          } ~
          pathPrefix("assignees") {
            (post & pathEnd & entity(as[CustomerAssignmentPayload])) { payload ⇒
              goodOrFailures {
                CustomerAssignmentUpdater.assign(admin, customerId, payload.assignees)
              }
            } ~
            (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
              goodOrFailures {
                CustomerAssignmentUpdater.unassign(admin, customerId, assigneeId)
              }
            }
          } ~
          pathPrefix("watchers") {
            (post & pathEnd & entity(as[CustomerWatchersPayload])) { payload ⇒
              goodOrFailures {
                CustomerWatcherUpdater.watch(admin, customerId, payload.watchers)
              }
            } ~
            (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
              goodOrFailures {
                CustomerWatcherUpdater.unwatch(admin, customerId, assigneeId)
              }
            }
          } ~
          pathPrefix("addresses") {
            (get & pathEnd & sortAndPage) { implicit sortAndPage ⇒
              goodOrFailures {
                AddressManager.findAllByCustomer(Originator(admin), customerId)
              }
            } ~
            (post & pathEnd & entity(as[CreateAddressPayload])) { payload ⇒
              goodOrFailures {
                AddressManager.create(Originator(admin), payload, customerId)
              }
            } ~
            (post & path(IntNumber / "default") & pathEnd) { addressId ⇒
              goodOrFailures {
                AddressManager.setDefaultShippingAddress(addressId, customerId)
              }
            } ~
            (get & path(IntNumber) & pathEnd) { addressId ⇒
              goodOrFailures {
                AddressManager.get(Originator(admin), addressId, customerId)
              }
            } ~
            (delete & path(IntNumber) & pathEnd) { addressId ⇒
              nothingOrFailures {
                AddressManager.remove(Originator(admin), addressId, customerId)
              }
            } ~
            (delete & path("default") & pathEnd) {
              nothingOrFailures {
                AddressManager.removeDefaultShippingAddress(customerId)
              }
            } ~
            (patch & path(IntNumber) & pathEnd & entity(as[CreateAddressPayload])) { (addressId, payload) ⇒
              activityContext(admin) { implicit ac ⇒
                goodOrFailures {
                  AddressManager.edit(Originator(admin), addressId, customerId, payload)
                }
              }
            }
          } ~
          pathPrefix("payment-methods" / "credit-cards") {
            (get & pathEnd) {
              complete {
                CreditCardManager.creditCardsInWalletFor(customerId)
              }
            } ~
            (post & path(IntNumber / "default") & pathEnd & entity(as[payloads.ToggleDefaultCreditCard])) { (cardId, payload) ⇒
              goodOrFailures {
                CreditCardManager.toggleCreditCardDefault(customerId, cardId, payload.isDefault)
              }
            } ~
            (post & pathEnd & entity(as[payloads.CreateCreditCard])) { payload ⇒
              goodOrFailures {
                CreditCardManager.createCardThroughGateway(customerId, payload, Some(admin))
              }
            } ~
            (patch & path(IntNumber) & pathEnd & entity(as[payloads.EditCreditCard])) { (cardId, payload) ⇒
              goodOrFailures {
                CreditCardManager.editCreditCard(customerId, cardId, payload, Some(admin))
              }
            } ~
            (delete & path(IntNumber) & pathEnd) { cardId ⇒
              nothingOrFailures {
                CreditCardManager.deleteCreditCard(customerId, cardId, Some(admin))
              }
            }
          } ~
          pathPrefix("payment-methods" / "store-credit") {
            pathPrefix("transactions") {
              (get & pathEnd & sortAndPage) { implicit sortAndPage ⇒
                goodOrFailures {
                  StoreCreditAdjustmentsService.forCustomer(customerId)
                }
              }
            } ~
            (get & path("totals")) {
              goodOrFailures {
                StoreCreditService.totalsForCustomer(customerId)
              }
            } ~
            (get & pathEnd & sortAndPage) { implicit sortAndPage ⇒
              goodOrFailures {
                StoreCreditService.findAllByCustomer(customerId)
              }
            } ~
            (post & pathEnd & entity(as[payloads.CreateManualStoreCredit])) { payload ⇒
              goodOrFailures {
                StoreCreditService.createManual(admin, customerId, payload)
              }
            } ~
            (post & path(IntNumber / "convert") & pathEnd) { storeCreditId ⇒
              goodOrFailures {
                CustomerCreditConverter.toGiftCard(storeCreditId, customerId, admin)
              }
            }
          }
        }
      }
    }
  }
}

