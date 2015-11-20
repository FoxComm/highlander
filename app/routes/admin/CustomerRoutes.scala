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
        } ~
        (get & pathPrefix("searchForNewOrder") & pathEnd &
          parameters('term).as(payloads.CustomerSearchForNewOrder)) { p ⇒
          (sortAndPage) { implicit sortAndPage ⇒
            goodOrFailures {
              CustomerManager.searchForNewOrder(p)
            }
           }
        } ~
        (post & pathEnd & entity(as[payloads.CreateCustomerPayload])) { payload ⇒
          goodOrFailures {
            CustomerManager.create(payload)
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
          goodOrFailures {
            CustomerManager.update(customerId, payload)
          }
        } ~
        (post & path("activate") & pathEnd & entity(as[ActivateCustomerPayload])) { payload ⇒
          goodOrFailures {
            CustomerManager.activate(customerId, payload)
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
        pathPrefix("addresses") {
          (get & pathEnd & sortAndPage) { implicit sortAndPage ⇒
            goodOrFailures {
              AddressManager.findAllVisibleByCustomer(customerId)
            }
          } ~
          (post & pathEnd & entity(as[CreateAddressPayload])) { payload ⇒
            goodOrFailures {
              AddressManager.create(payload, customerId)
            }
          } ~
          (post & path(IntNumber / "default") & pathEnd) { id ⇒
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
          (patch & path(IntNumber) & pathEnd & entity(as[CreateAddressPayload])) { (addressId, payload) ⇒
            goodOrFailures {
              AddressManager.edit(addressId, customerId, payload)
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
            complete {
              whenFound(Customers.findOneById(customerId).run()) { customer ⇒
                CreditCardManager.createCardThroughGateway(customer, payload)
              }
            }
          } ~
          (patch & path(IntNumber) & pathEnd & entity(as[payloads.EditCreditCard])) { (cardId, payload) ⇒
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
