package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import cats.data.Xor
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models._
import payloads._
import services._
import slick.driver.PostgresDriver.api._
import utils.Apis
import utils.Http._
import utils.Slick.implicits._
import utils.SprayDirectives._

import scala.concurrent.{ExecutionContext, Future}

object CustomerRoutes {

  def routes(implicit ec: ExecutionContext, db: Database,
    mat: Materializer, storeAdminAuth: AsyncAuthenticator[StoreAdmin], apis: Apis) = {

    authenticateBasicAsync(realm = "admin", storeAdminAuth) { admin ⇒

      pathPrefix("customers") {
        (get & pathEnd) {
          good {
            models.Customers.sortBy(_.firstName.desc).result.run()
          }
        }
      } ~
      pathPrefix("customers" / IntNumber) { customerId ⇒
        (get & pathEnd) {
          goodOrNotFound {
            models.Customers.findById(customerId)
          }
        } ~
        (post & path("disable") & entity(as[payloads.ToggleCustomerDisabled])) { payload ⇒
          goodOrFailures {
            CustomerManager.toggleDisabled(customerId, payload.disabled, admin)
          }
        } ~
        pathPrefix("addresses") {
          (get & pathEnd) {
            good {
              Addresses._findAllByCustomerIdWithRegions(customerId).result.run().map { records ⇒
                responses.Addresses.build(records)
              }
            }
          } ~
          (post & entity(as[CreateAddressPayload]) & pathEnd) { payload ⇒
            goodOrFailures {
              AddressManager.create(payload, customerId)
            }
          } ~
          (post & path(IntNumber / "default") & entity(as[payloads.ToggleDefaultShippingAddress]) & pathEnd) {
            (id, payload) ⇒
              nothingOrFailures {
                AddressManager.setDefaultShippingAddress(customerId, id)
              }
          } ~
          (post & path(IntNumber / "default") & entity(as[payloads.ToggleDefaultShippingAddress]) & pathEnd) {
            (id, payload) ⇒
              nothingOrFailures {
                AddressManager.setDefaultShippingAddress(customerId, id)
              }
          } ~
          (delete & path(IntNumber) & pathEnd)  {
            (id) ⇒
              nothingOrFailures {
                AddressManager.remove(customerId, id)
              }
          } ~
          (delete & path("default") & pathEnd) {
            nothingOrFailures {
              AddressManager.removeDefaultShippingAddress(customerId)
            }
          } ~
          (patch & path(IntNumber) & entity(as[CreateAddressPayload]) & pathEnd) { (addressId, payload) ⇒
            goodOrFailures {
              AddressManager.edit(addressId, customerId, payload)
            }
          } ~
          (get & path("display") & pathEnd) {
            complete {
              Customers._findById(customerId).result.headOption.run().flatMap {
                case None           ⇒ Future.successful(notFoundResponse)
                case Some(customer) ⇒ AddressManager.getDisplayAddress(customer).map(renderOrNotFound(_))
              }
            }
          }
        } ~
        pathPrefix("payment-methods" / "credit-cards") {
          (get & pathEnd) {
            good { CreditCardManager.creditCardsInWalletFor(customerId) }
          } ~
          (post & path(IntNumber / "default") & entity(as[payloads.ToggleDefaultCreditCard]) & pathEnd) {
            (cardId, payload) ⇒
              goodOrFailures {
                CreditCardManager.toggleCreditCardDefault(customerId, cardId, payload.isDefault)
              }
          } ~
          (post & entity(as[payloads.CreateCreditCard]) & pathEnd) { payload ⇒
            complete {
              whenFound(Customers.findById(customerId)) { customer ⇒
                CreditCardManager.createCardThroughGateway(customer, payload)
              }
            }
          } ~
          (patch & path(IntNumber) & entity(as[payloads.EditCreditCard]) & pathEnd) { (cardId, payload) ⇒
            nothingOrFailures {
              CreditCardManager.editCreditCard(customerId, cardId, payload)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { cardId ⇒
            nothingOrFailures {
              CreditCardManager.deleteCreditCard(customerId = customerId, id = cardId)
            }
          }
        } ~
        pathPrefix("payment-methods" / "store-credit") {
          (get & pathEnd) {
            complete {
              whenFound(Customers.findById(customerId)) { customer ⇒
                StoreCredits.findAllByCustomerId(customer.id).map(Xor.right)
              }
            }
          } ~
          (post & entity(as[payloads.CreateManualStoreCredit])) { payload ⇒
            goodOrFailures {
              StoreCreditService.createManual(admin, customerId, payload)
            }
          } ~
          (post & path(IntNumber / "convert")) { storeCreditId ⇒
            complete {
              whenFoundDispatchToService(StoreCredits.findById(storeCreditId).run()) { sc ⇒
                CustomerCreditConverter.toGiftCard(sc, customerId)
              }
            }
          }
        }
      }
    }
  }
}
