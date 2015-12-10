package routes.admin

import scala.concurrent.{ExecutionContext, Future}

import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.{Customers, StoreAdmin}
import payloads.{ActivateCustomerPayload, CreateAddressPayload, UpdateCustomerPayload}
import services.{AddressManager, CreditCardManager, CustomerCreditConverter, CustomerManager, StoreCreditAdjustmentsService, StoreCreditService}
import slick.driver.PostgresDriver.api._
import utils.Apis
import utils.CustomDirectives._
import utils.Http._
import utils.Slick.implicits._

object CustomerRoutes {

  def routes(implicit ec: ExecutionContext, db: Database,
    mat: Materializer, storeAdminAuth: AsyncAuthenticator[StoreAdmin], apis: Apis) = {

    authenticateBasicAsync(realm = "admin", storeAdminAuth) { admin ⇒

      pathPrefix("customers") {
        (get & pathEnd & sortAndPage) { implicit sortAndPage ⇒
          goodOrFailures {
            CustomerManager.findAll
          }
        } ~
        (get & pathPrefix("searchForNewOrder") & pathEnd &
          parameters('term).as(payloads.CustomerSearchForNewOrder)) { p ⇒
          sortAndPage { implicit sortAndPage ⇒
            goodOrFailures {
              CustomerManager.searchForNewOrder(p)
            }
           }
        } ~
        (post & pathEnd & entity(as[payloads.CreateCustomerPayload])) { payload ⇒
          activityContext(admin) { implicit ac ⇒
            goodOrFailures {
              CustomerManager.create(payload, Some(admin))
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
        (patch & pathEnd & entity(as[UpdateCustomerPayload])) { payload ⇒
          activityContext(admin) { implicit ac ⇒
            goodOrFailures {
              CustomerManager.update(customerId, payload, admin)
            }
          }
        } ~
        (post & path("activate") & pathEnd & entity(as[ActivateCustomerPayload])) { payload ⇒
          activityContext(admin) { implicit ac ⇒
            goodOrFailures {
              CustomerManager.activate(customerId, payload, admin)
            }
          }
        } ~
        (post & path("disable") & pathEnd & entity(as[payloads.ToggleCustomerDisabled])) { payload ⇒
          activityContext(admin) { implicit ac ⇒
            goodOrFailures {
              CustomerManager.toggleDisabled(customerId, payload.disabled, admin)
            }
          }
        } ~
        (post & path("blacklist") & pathEnd & entity(as[payloads.ToggleCustomerBlacklisted])) { payload ⇒
          activityContext(admin) { implicit ac ⇒
            goodOrFailures {
              CustomerManager.toggleBlacklisted(customerId, payload.blacklisted, admin)
            }
          }
        } ~
        pathPrefix("addresses") {
          (get & pathEnd & sortAndPage) { implicit sortAndPage ⇒
            goodOrFailures {
              AddressManager.findAllVisibleByCustomer(customerId)
            }
          } ~
          (post & pathEnd & entity(as[CreateAddressPayload])) { payload ⇒
            activityContext(admin) { implicit ac ⇒
              goodOrFailures {
                AddressManager.create(payload, customerId, Some(admin))
              }
            }
          } ~
          (post & path(IntNumber / "default") & pathEnd) { id ⇒
            nothingOrFailures {
              AddressManager.setDefaultShippingAddress(customerId, id)
            }
          } ~
          (get & path(IntNumber) & pathEnd) { id ⇒
            goodOrFailures {
              AddressManager.get(customerId, id)
            }
          } ~
          (delete & path(IntNumber) & pathEnd)  { id ⇒
            activityContext(admin) { implicit ac ⇒
              nothingOrFailures {
                AddressManager.remove(customerId, id, admin)
              }
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
                AddressManager.edit(addressId, customerId, payload, admin)
              }
            }
          } ~
          (get & path("display") & pathEnd) {
            complete {
              Customers.findById(customerId).result.headOption.run().flatMap {
                case None           ⇒ Future.successful(notFoundResponse)
                case Some(customer) ⇒ AddressManager.getDisplayAddress(customer).map(renderOrBadRequest(_))
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
          (post & path(IntNumber / "default") & pathEnd & entity(as[payloads.ToggleDefaultCreditCard])) {
            (cardId, payload) ⇒
              goodOrFailures {
                CreditCardManager.toggleCreditCardDefault(customerId, cardId, payload.isDefault)
              }
          } ~
          (post & pathEnd & entity(as[payloads.CreateCreditCard])) { payload ⇒
            activityContext(admin) { implicit ac ⇒
              complete {
                whenFound(Customers.findOneById(customerId).run()) { customer ⇒
                  CreditCardManager.createCardThroughGateway(admin, customer, payload)
                }
              }
            }
          } ~
          (patch & path(IntNumber) & pathEnd & entity(as[payloads.EditCreditCard])) { (cardId, payload) ⇒
            activityContext(admin) { implicit ac ⇒
              nothingOrFailures {
                CreditCardManager.editCreditCard(admin, customerId, cardId, payload)
              }
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { cardId ⇒
            activityContext(admin) { implicit ac ⇒
              nothingOrFailures {
                CreditCardManager.deleteCreditCard(admin = admin, customerId = customerId, id = cardId)
              }
            }
          }
        } ~
        pathPrefix("payment-methods" / "store-credit") {
          pathPrefix("transactions") {
            (get & pathEnd & sortAndPage) { implicit sortAndPage ⇒
              complete {
                StoreCreditAdjustmentsService.forCustomer(customerId).map(renderMetadataResult)
              }
            }
          } ~
          (get & path("totals")) {
            goodOrFailures {
              StoreCreditService.totalsForCustomer(customerId)
            }
          } ~
          (get & pathEnd & sortAndPage) { implicit sortAndPage ⇒
            complete {
              StoreCreditService.findAllByCustomer(customerId).map(renderMetadataResult)
            }
          } ~
          (post & pathEnd & entity(as[payloads.CreateManualStoreCredit])) { payload ⇒
            activityContext(admin) { implicit ac ⇒
              goodOrFailures {
                StoreCreditService.createManual(admin, customerId, payload)
              }
            }
          } ~
          (post & path(IntNumber / "convert") & pathEnd) { storeCreditId ⇒
            activityContext(admin) { implicit ac ⇒
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
