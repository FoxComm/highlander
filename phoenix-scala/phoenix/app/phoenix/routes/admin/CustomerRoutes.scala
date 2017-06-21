package phoenix.routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import phoenix.models.account.User
import phoenix.payloads.AddressPayloads.CreateAddressPayload
import phoenix.payloads.CartPayloads.CheckoutCart
import phoenix.payloads.CustomerGroupPayloads.AddCustomerToGroups
import phoenix.payloads.CustomerPayloads._
import phoenix.payloads.PaymentPayloads._
import phoenix.payloads.UserPayloads._
import phoenix.services.Authenticator.AuthData
import phoenix.services._
import phoenix.services.account._
import phoenix.services.carts.CartQueries
import phoenix.services.customerGroups.GroupMemberManager
import phoenix.services.customers._
import phoenix.utils.FoxConfig.config
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis
import phoenix.utils.http.CustomDirectives._
import phoenix.utils.http.Http._
import phoenix.utils.http.JsonSupport._

object CustomerRoutes {

  def routes(implicit ec: EC, db: DB, auth: AuthData[User], apis: Apis): Route = {

    activityContext(auth) { implicit ac ⇒
      determineObjectContext(db, ec) { implicit ctx ⇒
        pathPrefix("customers") {
          (post & pathEnd & entity(as[CreateCustomerPayload])) { payload ⇒
            mutateOrFailures {
              val roleName = config.users.customer.role
              val orgName  = config.users.customer.org
              val scopeId  = config.users.customer.scopeId

              val context = AccountCreateContext(List(roleName), orgName, scopeId)
              CustomerManager.createFromAdmin(payload, Some(auth.model), context)
            }
          }
        } ~
        pathPrefix("customers" / IntNumber) { accountId ⇒
          (get & pathEnd) {
            getOrFailures {
              CustomerManager.getByAccountId(accountId)
            }
          } ~
          (get & path("cart")) {
            getOrFailures {
              CartQueries.findOrCreateCartByAccountId(accountId, ctx, Some(auth.model))
            }
          } ~
          (post & path("checkout") & pathEnd & entity(as[CheckoutCart])) { payload ⇒
            mutateOrFailures {
              Checkout.forAdminOneClick(accountId, payload)
            }
          } ~
          (patch & pathEnd & entity(as[UpdateCustomerPayload])) { payload ⇒
            mutateOrFailures {
              CustomerManager.updateFromAdmin(accountId, payload, Some(auth.model))
            }
          } ~
          (post & path("activate") & pathEnd & entity(as[ActivateCustomerPayload])) { payload ⇒
            mutateOrFailures {
              CustomerManager.activate(accountId, payload, auth.model)
            }
          } ~
          (post & path("disable") & pathEnd & entity(as[ToggleUserDisabled])) { payload ⇒
            mutateOrFailures {
              CustomerManager.toggleDisabled(accountId, payload.disabled, auth.model)
            }
          } ~
          (post & path("blacklist") & pathEnd & entity(as[ToggleUserBlacklisted])) { payload ⇒
            mutateOrFailures {
              CustomerManager.toggleBlacklisted(accountId, payload.blacklisted, auth.model)
            }
          } ~
          pathPrefix("addresses") {
            (get & pathEnd) {
              getOrFailures {
                AddressManager.findAllByAccountId(accountId)
              }
            } ~
            (post & pathEnd & entity(as[CreateAddressPayload])) { payload ⇒
              mutateOrFailures {
                AddressManager.create(auth.model, payload, accountId)
              }
            } ~
            (post & path(IntNumber / "default") & pathEnd) { addressId ⇒
              mutateOrFailures {
                AddressManager.setDefaultShippingAddress(addressId, accountId)
              }
            } ~
            (get & path(IntNumber) & pathEnd) { addressId ⇒
              getOrFailures {
                AddressManager.get(auth.model, addressId, accountId)
              }
            } ~
            (delete & path(IntNumber) & pathEnd) { addressId ⇒
              deleteOrFailures {
                AddressManager.remove(auth.model, addressId, accountId)
              }
            } ~
            (delete & path("default") & pathEnd) {
              deleteOrFailures {
                AddressManager.removeDefaultShippingAddress(accountId)
              }
            } ~
            (patch & path(IntNumber) & pathEnd & entity(as[CreateAddressPayload])) { (addressId, payload) ⇒
              mutateOrFailures {
                AddressManager.edit(auth.model, addressId, accountId, payload)
              }
            }
          } ~
          pathPrefix("payment-methods" / "credit-cards") {
            (get & pathEnd) {
              complete {
                CreditCardManager.creditCardsInWalletFor(accountId)
              }
            } ~
            (post & path(IntNumber / "default") & pathEnd) { cardId ⇒
              mutateOrFailures {
                CreditCardManager.setDefaultCreditCard(accountId, cardId)
              }
            } ~
            (post & pathEnd & entity(as[CreateCreditCardFromTokenPayload])) { payload ⇒
              mutateOrFailures {
                CreditCardManager.createCardFromToken(accountId, payload, Some(auth.model))
              }
            } ~
            (patch & path(IntNumber) & pathEnd & entity(as[EditCreditCard])) { (cardId, payload) ⇒
              mutateOrFailures {
                CreditCardManager.editCreditCard(accountId, cardId, payload, Some(auth.model))
              }
            } ~
            (delete & path(IntNumber) & pathEnd) { cardId ⇒
              deleteOrFailures {
                CreditCardManager.deleteCreditCard(accountId, cardId, Some(auth.model))
              }
            } ~
            (delete & path("default") & pathEnd) {
              deleteOrFailures {
                CreditCardManager.removeDefaultCreditCard(accountId)
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
                StoreCreditService.createManual(auth.model, accountId, payload)
              }
            } ~
            (post & path("custom") & pathEnd & entity(as[CreateExtensionStoreCredit])) { payload ⇒
              mutateOrFailures {
                // TODO: prohibit access from non-extensions? by user probably?
                StoreCreditService.createFromExtension(auth.model, accountId, payload)
              }
            } ~
            (post & path(IntNumber / "convert") & pathEnd) { storeCreditId ⇒
              mutateOrFailures {
                CustomerCreditConverter.toGiftCard(storeCreditId, accountId, auth.model)
              }
            }
          } ~
          pathPrefix("customer-groups") {
            (post & pathEnd & entity(as[AddCustomerToGroups])) { payload ⇒
              mutateOrFailures {
                GroupMemberManager.addCustomerToGroups(accountId, payload.groups)
              }
            }
          }
        }
      }
    }
  }
}
