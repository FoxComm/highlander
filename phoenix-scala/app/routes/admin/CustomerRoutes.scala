package routes.admin

import akka.http.scaladsl.server.Directives._

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.account.User
import payloads.AddressPayloads.CreateAddressPayload
import payloads.CustomerPayloads._
import payloads.UserPayloads._
import payloads.PaymentPayloads._
import services.carts.CartQueries
import services.account._
import services.customers._
import services.account._
import services.{AddressManager, CreditCardManager, CustomerCreditConverter, StoreCreditService}
import services.Authenticator.AuthData
import utils.aliases._
import utils.apis.Apis
import utils.http.CustomDirectives._
import utils.http.Http._
import utils.FoxConfig._

object CustomerRoutes {

  def routes(implicit ec: EC, db: DB, auth: AuthData[User], apis: Apis, tr: TR, tracer: TEI) = {

    activityContext(auth.model) { implicit ac ⇒
      pathPrefix("customers") {
        (post & pathEnd & entity(as[CreateCustomerPayload])) { payload ⇒
          mutateOrFailures {
            val roleName = config.getString(s"user.customer.role")
            val orgName  = config.getString(s"user.customer.org")
            val scopeId  = config.getInt(s"user.customer.scope_id")

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
          determineObjectContext(db, ec) { implicit ctx ⇒
            getOrFailures {
              CartQueries.findOrCreateCartByAccountId(accountId, ctx, Some(auth.model))
            }
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
          (patch & path(IntNumber) & pathEnd & entity(as[CreateAddressPayload])) {
            (addressId, payload) ⇒
              activityContext(auth.model) { implicit ac ⇒
                mutateOrFailures {
                  AddressManager.edit(auth.model, addressId, accountId, payload)
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
        }
      }
    }
  }
}
