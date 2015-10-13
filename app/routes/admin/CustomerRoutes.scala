package routes.admin

import akka.http.scaladsl.model.{HttpResponse, ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.stream.stage.{Context, PushPullStage}
import akka.util.ByteString
import cats.data.Xor
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models._
import org.json4s.{Formats, jackson}
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.{write ⇒ json}
import payloads._
import services._
import slick.driver.PostgresDriver.api._
import utils.Apis
import utils.Http._
import utils.Slick.implicits._
import utils.CustomDirectives._

import scala.concurrent.{ExecutionContext, Future}

object CustomerRoutes {

  def routes(implicit ec: ExecutionContext, db: Database,
    mat: Materializer, storeAdminAuth: AsyncAuthenticator[StoreAdmin], apis: Apis) = {

    authenticateBasicAsync(realm = "admin", storeAdminAuth) { admin ⇒

      pathPrefix("customers") {
        (get & pathEnd & sortAndPage) { implicit sortAndPage ⇒
          goodOrFailures {
            CustomerManager.findAll
          }
        }
      } ~
      pathPrefix("customers" / IntNumber) { customerId ⇒
        (get & pathEnd) {
          goodOrFailures {
            CustomerManager.getById(customerId)
          }
        } ~
        (post & path("disable") & entity(as[payloads.ToggleCustomerDisabled])) { payload ⇒
          goodOrFailures {
            CustomerManager.toggleDisabled(customerId, payload.disabled, admin)
          }
        } ~
        pathPrefix("addresses") {
          (get & pathEnd & sortAndPage) { implicit sortAndPage ⇒
            goodOrFailures {
              AddressManager.findAllByCustomer(customerId)
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
          (get & path(IntNumber) & pathEnd)  {
            id ⇒
              goodOrFailures {
                AddressManager.get(customerId, id)
              }
          } ~
          (delete & path(IntNumber) & pathEnd)  {
            id ⇒
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
          (get & pathEnd & sortAndPage) { implicit sortAndPage ⇒
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
          (get & pathEnd & sortAndPage) { implicit sortAndPage ⇒
            complete {
              whenFound(Customers.findById(customerId)) { customer ⇒
                StoreCreditService.findAllByCustomer(customer.id)
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
