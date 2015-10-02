package routes.admin

import scala.concurrent.{ExecutionContext, Future}
import scala.collection.immutable.Seq
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer

import cats.data.Xor
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import models._
import payloads._
import responses.{AllOrders, BulkOrderUpdateResponse, AdminNotes, FullOrder}
import services._
import slick.driver.PostgresDriver.api._
import utils.Apis
import utils.Slick
import utils.Slick.DbResult
import utils.Slick.implicits._
import Json4sSupport._
import utils.Http._

object CustomerRoutes {

  def routes(implicit ec: ExecutionContext, db: Database,
    mat: Materializer, storeAdminAuth: AsyncAuthenticator[StoreAdmin], apis: Apis) = {

    authenticateBasicAsync(realm = "admin", storeAdminAuth) { admin ⇒

      pathPrefix("customers") {
        (get & pathEnd) {
          complete {
            models.Customers.sortBy(_.firstName.desc).result.run().map(render(_))
          }
        }
      } ~
      pathPrefix("customers" / IntNumber) { customerId ⇒
        (get & pathEnd) {
          complete {
            renderOrNotFound(models.Customers.findById(customerId))
          }
        } ~
        (post & path("disable") & entity(as[payloads.ToggleCustomerDisabled])) { payload ⇒
          complete {
            CustomerManager.toggleDisabled(customerId, payload.disabled, admin).map(renderGoodOrFailures)
          }
        } ~
        pathPrefix("addresses") {
          (get & pathEnd) {
            complete {
              Addresses._findAllByCustomerIdWithRegions(customerId).result.run().map { records ⇒
                render(responses.Addresses.build(records))
              }
            }
          } ~
          (post & entity(as[CreateAddressPayload]) & pathEnd) { payload ⇒
            complete {
              AddressManager.create(payload, customerId).map(renderGoodOrFailures)
            }
          } ~
          (post & path(IntNumber / "default") & entity(as[payloads.ToggleDefaultShippingAddress]) & pathEnd) {
            (id, payload) ⇒
              complete {
                AddressManager.setDefaultShippingAddress(customerId, id).map(renderNothingOrFailures)
              }
          } ~
          (delete & path("default") & pathEnd) {
            complete {
              AddressManager.removeDefaultShippingAddress(customerId).map(renderNothingOrFailures)
            }
          } ~
          (patch & path(IntNumber) & entity(as[CreateAddressPayload]) & pathEnd) { (addressId, payload) ⇒
            complete {
              AddressManager.edit(addressId, customerId, payload).map(renderGoodOrFailures)
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
            complete { CreditCardManager.creditCardsInWalletFor(customerId).map(render(_)) }
          } ~
          (post & path(IntNumber / "default") & entity(as[payloads.ToggleDefaultCreditCard]) & pathEnd) {
            (cardId, payload) ⇒
              complete {
                val result = CreditCardManager.toggleCreditCardDefault(customerId, cardId, payload.isDefault)
                result.map(renderGoodOrFailures)
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
            complete {
              CreditCardManager.editCreditCard(customerId, cardId, payload).map(renderNothingOrFailures)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { cardId ⇒
            complete {
              CreditCardManager.deleteCreditCard(customerId = customerId, id = cardId).map(renderNothingOrFailures)
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
            complete {
              StoreCreditService.createManual(admin, customerId, payload).map(renderGoodOrFailures)
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
