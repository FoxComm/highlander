package routes.admin

import akka.http.scaladsl.server.Directives._

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.StoreAdmin
import models.traits.Originator
import payloads.AddressPayloads.CreateAddressPayload
import payloads.CustomerPayloads._
import payloads.PaymentPayloads._
import services.carts.CartQueries
import services.customers._
import services.{AddressManager, CreditCardManager, CustomerCreditConverter, StoreCreditService}
import utils.aliases._
import utils.apis.Apis
import utils.http.CustomDirectives._
import utils.http.Http._

object CustomerRoutes {

  def routes(implicit ec: EC, db: DB, admin: StoreAdmin, apis: Apis) = {

    activityContext(admin) { implicit ac ⇒
      pathPrefix("customers") {
        (post & pathEnd & entity(as[CreateCustomerPayload])) { payload ⇒
          mutateOrFailures {
            CustomerManager.create(payload, Some(admin))
          }
        }
      } ~
      pathPrefix("customers" / IntNumber) { customerId ⇒
        (get & pathEnd) {
          getOrFailures {
            CustomerManager.getById(customerId)
          }
        } ~
        (get & path("cart")) {
          determineObjectContext(db, ec) { implicit ctx ⇒
            getOrFailures {
              CartQueries.findOrCreateCartByCustomerId(customerId, ctx, Some(admin))
            }
          }
        } ~
        (patch & pathEnd & entity(as[UpdateCustomerPayload])) { payload ⇒
          mutateOrFailures {
            CustomerManager.update(customerId, payload, Some(admin))
          }
        } ~
        (post & path("activate") & pathEnd & entity(as[ActivateCustomerPayload])) { payload ⇒
          mutateOrFailures {
            CustomerManager.activate(customerId, payload, admin)
          }
        } ~
        (post & path("disable") & pathEnd & entity(as[ToggleCustomerDisabled])) { payload ⇒
          mutateOrFailures {
            CustomerManager.toggleDisabled(customerId, payload.disabled, admin)
          }
        } ~
        (post & path("blacklist") & pathEnd & entity(as[ToggleCustomerBlacklisted])) { payload ⇒
          mutateOrFailures {
            CustomerManager.toggleBlacklisted(customerId, payload.blacklisted, admin)
          }
        } ~
        pathPrefix("addresses") {
          (get & pathEnd) {
            getOrFailures {
              AddressManager.findAllByCustomer(customerId)
            }
          } ~
          (post & pathEnd & entity(as[CreateAddressPayload])) { payload ⇒
            mutateOrFailures {
              AddressManager.create(Originator(admin), payload, customerId)
            }
          } ~
          (post & path(IntNumber / "default") & pathEnd) { addressId ⇒
            mutateOrFailures {
              AddressManager.setDefaultShippingAddress(addressId, customerId)
            }
          } ~
          (get & path(IntNumber) & pathEnd) { addressId ⇒
            getOrFailures {
              AddressManager.get(Originator(admin), addressId, customerId)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { addressId ⇒
            deleteOrFailures {
              AddressManager.remove(Originator(admin), addressId, customerId)
            }
          } ~
          (delete & path("default") & pathEnd) {
            deleteOrFailures {
              AddressManager.removeDefaultShippingAddress(customerId)
            }
          } ~
          (patch & path(IntNumber) & pathEnd & entity(as[CreateAddressPayload])) {
            (addressId, payload) ⇒
              activityContext(admin) { implicit ac ⇒
                mutateOrFailures {
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
          (post & path(IntNumber / "default") & pathEnd & entity(as[ToggleDefaultCreditCard])) {
            (cardId, payload) ⇒
              mutateOrFailures {
                CreditCardManager.toggleCreditCardDefault(customerId, cardId, payload.isDefault)
              }
          } ~
          (post & pathEnd & entity(as[CreateCreditCardFromTokenPayload])) { payload ⇒
            mutateOrFailures {
              CreditCardManager.createCardFromToken(customerId, payload, Some(admin))
            }
          } ~
          (patch & path(IntNumber) & pathEnd & entity(as[EditCreditCardPayload])) {
            (cardId, payload) ⇒
              mutateOrFailures {
                CreditCardManager.editCreditCard(customerId, cardId, payload, Some(admin))
              }
          } ~
          (delete & path(IntNumber) & pathEnd) { cardId ⇒
            deleteOrFailures {
              CreditCardManager.deleteCreditCard(customerId, cardId, Some(admin))
            }
          }
        } ~
        pathPrefix("payment-methods" / "store-credit") {
          (get & path("totals")) {
            getOrFailures {
              StoreCreditService.totalsForCustomer(customerId)
            }
          } ~
          (post & pathEnd & entity(as[CreateManualStoreCredit])) { payload ⇒
            mutateOrFailures {
              StoreCreditService.createManual(admin, customerId, payload)
            }
          } ~
          (post & path("custom") & pathEnd & entity(as[CreateExtensionStoreCredit])) { payload ⇒
            mutateOrFailures {
              // TODO: prohibit access from non-extensions? by user probably?
              StoreCreditService.createFromExtension(admin, customerId, payload)
            }
          } ~
          (post & path(IntNumber / "convert") & pathEnd) { storeCreditId ⇒
            mutateOrFailures {
              CustomerCreditConverter.toGiftCard(storeCreditId, customerId, admin)
            }
          }
        }
      }
    }
  }
}
