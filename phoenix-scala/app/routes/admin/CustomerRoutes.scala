package routes.admin

import akka.http.scaladsl.server.Directives._

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.StoreAdmin
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
      pathPrefix("customers" / IntNumber) { accountId ⇒
        (get & pathEnd) {
          getOrFailures {
            CustomerManager.getById(accountId)
          }
        } ~
        (get & path("cart")) {
          determineObjectContext(db, ec) { implicit ctx ⇒
            getOrFailures {
              CartQueries.findOrCreateCartByCustomerId(accountId, ctx, Some(admin))
            }
          }
        } ~
        (patch & pathEnd & entity(as[UpdateCustomerPayload])) { payload ⇒
          mutateOrFailures {
            CustomerManager.update(accountId, payload, Some(admin))
          }
        } ~
        (post & path("activate") & pathEnd & entity(as[ActivateCustomerPayload])) { payload ⇒
          mutateOrFailures {
            CustomerManager.activate(accountId, payload, admin)
          }
        } ~
        (post & path("disable") & pathEnd & entity(as[ToggleCustomerDisabled])) { payload ⇒
          mutateOrFailures {
            CustomerManager.toggleDisabled(accountId, payload.disabled, admin)
          }
        } ~
        (post & path("blacklist") & pathEnd & entity(as[ToggleCustomerBlacklisted])) { payload ⇒
          mutateOrFailures {
            CustomerManager.toggleBlacklisted(accountId, payload.blacklisted, admin)
          }
        } ~
        pathPrefix("addresses") {
          (get & pathEnd) {
            getOrFailures {
              AddressManager.findAllByCustomer(admin, accountId)
            }
          } ~
          (post & pathEnd & entity(as[CreateAddressPayload])) { payload ⇒
            mutateOrFailures {
              AddressManager.create(admin, payload, accountId)
            }
          } ~
          (post & path(IntNumber / "default") & pathEnd) { addressId ⇒
            mutateOrFailures {
              AddressManager.setDefaultShippingAddress(addressId, accountId)
            }
          } ~
          (get & path(IntNumber) & pathEnd) { addressId ⇒
            getOrFailures {
              AddressManager.get(admin, addressId, accountId)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { addressId ⇒
            deleteOrFailures {
              AddressManager.remove(admin, addressId, accountId)
            }
          } ~
          (delete & path("default") & pathEnd) {
            deleteOrFailures {
              AddressManager.removeDefaultShippingAddress(accountId)
            }
          } ~
          (patch & path(IntNumber) & pathEnd & entity(as[CreateAddressPayload])) {
            (addressId, payload) ⇒
              activityContext(admin) { implicit ac ⇒
                mutateOrFailures {
                  AddressManager.edit(admin, addressId, accountId, payload)
                }
              }
          }
        } ~
        pathPrefix("payment-methods" / "credit-cards") {
          (get & pathEnd) {
            complete {
              CreditCardManager.creditCardsInWalletFor(accountId)
            }
          } ~
          (post & path(IntNumber / "default") & pathEnd & entity(as[ToggleDefaultCreditCard])) {
            (cardId, payload) ⇒
              mutateOrFailures {
                CreditCardManager.toggleCreditCardDefault(accountId, cardId, payload.isDefault)
              }
          } ~
          (post & pathEnd & entity(as[CreateCreditCard])) { payload ⇒
            mutateOrFailures {
              CreditCardManager.createCardThroughGateway(accountId, payload, Some(admin))
            }
          } ~
          (patch & path(IntNumber) & pathEnd & entity(as[EditCreditCard])) { (cardId, payload) ⇒
            mutateOrFailures {
              CreditCardManager.editCreditCard(accountId, cardId, payload, Some(admin))
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { cardId ⇒
            deleteOrFailures {
              CreditCardManager.deleteCreditCard(accountId, cardId, Some(admin))
            }
          }
        } ~
        pathPrefix("payment-methods" / "store-credit") {
          (get & path("totals")) {
            getOrFailures {
              StoreCreditService.totalsForCustomer(accountId)
            }
          } ~
          (post & pathEnd & entity(as[CreateManualStoreCredit])) { payload ⇒
            mutateOrFailures {
              StoreCreditService.createManual(admin, accountId, payload)
            }
          } ~
          (post & path("custom") & pathEnd & entity(as[CreateExtensionStoreCredit])) { payload ⇒
            mutateOrFailures {
              // TODO: prohibit access from non-extensions? by user probably?
              StoreCreditService.createFromExtension(admin, accountId, payload)
            }
          } ~
          (post & path(IntNumber / "convert") & pathEnd) { storeCreditId ⇒
            mutateOrFailures {
              CustomerCreditConverter.toGiftCard(storeCreditId, accountId, admin)
            }
          }
        }
      }
    }
  }
}
