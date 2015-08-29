package routes

import scala.concurrent.{ExecutionContext, Future}
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer

import cats.data.Xor
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import models._
import akka.http.scaladsl.model.StatusCodes._

import payloads._
import responses.{AllOrders, AllOrdersWithFailures, AdminNotes, FullOrder}
import services._
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}
import utils.RunOnDbIO

object Admin {
  def routes(implicit ec: ExecutionContext, db: Database,
    mat: Materializer, storeAdminAuth: AsyncAuthenticator[StoreAdmin]) = {
    import Json4sSupport._
    import utils.Http._

    authenticateBasicAsync(realm = "admin", storeAdminAuth) { admin =>
      pathPrefix("gift-cards") {
        (get & pathEnd) {
          complete {
            models.GiftCards.sortBy(_.id.desc).result.run().map(render(_))
          }
        } ~
        (get & path(IntNumber) & pathEnd) { giftCardId ⇒
          complete {
            renderOrNotFound(GiftCards.findById(giftCardId).run())
          }
        }
      } ~
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
          (post & entity(as[CreateAddressPayload]) & pathEnd) { payload =>
            complete {
              AddressManager.create(payload, customerId).map(renderGoodOrFailures)
            }
          } ~
          (post & path(IntNumber / "default") & entity(as[payloads.ToggleDefaultShippingAddress]) & pathEnd) {
            (id, payload) ⇒
              complete {
                AddressManager.setDefaultShippingAddress(customerId, id).map { optFailure ⇒
                  optFailure.fold(HttpResponse(OK)) { f ⇒ renderFailure(Seq(f)) }
                }
              }
          } ~
          (delete & path("default") & pathEnd) {
            complete {
              AddressManager.removeDefaultShippingAddress(customerId).map { _ ⇒ noContentResponse }
            }
          } ~
          (patch & path(IntNumber) & entity(as[CreateAddressPayload]) & pathEnd) { (addressId, payload) =>
            complete {
              AddressManager.edit(addressId, customerId, payload).map(renderGoodOrFailures)
            }
          }
        } ~
        pathPrefix("payment-methods" / "credit-cards") {
          (get & pathEnd) {
            complete { CustomerManager.creditCardsInWalletFor(customerId).map(render(_)) }
          } ~
          (post & path(IntNumber / "default") & entity(as[payloads.ToggleDefaultCreditCard])) { (cardId, payload) ⇒
            complete {
              val result = CustomerManager.toggleCreditCardDefault(customerId, cardId, payload.isDefault)
              result.map(renderGoodOrFailures)
            }
          } ~
          (patch & path(IntNumber) & entity(as[payloads.EditCreditCard])) { (cardId, payload) ⇒
            complete {
              CustomerManager.editCreditCard(customerId, cardId, payload).map(renderNothingOrFailures)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { cardId ⇒
            complete {
              CustomerManager.deleteCreditCard(customerId = customerId, id = cardId).map(renderNothingOrFailures)
            }
          }
        } ~
        pathPrefix("payment-methods" / "store-credit") {
          (get & pathEnd) {
            complete { StoreCredits.findAllByCustomerId(customerId).map(render(_)) }
          } ~
          (get & path(IntNumber)) { storeCreditId ⇒
            complete { StoreCredits.findById(storeCreditId).run().map(renderOrNotFound(_)) }
          } ~
          (post & path(IntNumber / "convert")) { storeCreditId ⇒
            complete {
              whenFoundDispatchToService(StoreCredits.findById(storeCreditId).run()) { sc ⇒
                CustomerCreditConverter.toGiftCard(sc, customerId)
              }
            }
          }
        }
      } ~
      pathPrefix("orders") {
        (get & pathEnd) {
          complete {
            AllOrders.findAll
          }
        } ~
        (patch & entity(as[BulkUpdateOrdersPayload]) & pathEnd) { payload ⇒
          complete {
            for {
              failures ← OrderUpdater.updateStatuses(payload.referenceNumbers, payload.status)
              orders ← AllOrders.findAll
            } yield AllOrdersWithFailures(orders, failures)
          }
        }
      } ~
      pathPrefix("orders" / """([a-zA-Z0-9-_]*)""".r) { refNum ⇒
        (get & pathEnd) {
          complete {
            whenFound(Orders.findByRefNum(refNum).result.headOption.run()) { order ⇒
              FullOrder.fromOrder(order).map(Xor.right)
            }
          }
        } ~
        (patch & entity(as[UpdateOrderPayload])) { payload ⇒
          complete {
            whenOrderFoundAndEditable(refNum) { _ ⇒
              OrderUpdater.updateStatus(refNum, payload.status)
            }
          }
        } ~
        (post & path("increase-remorse-period") & pathEnd) {
          complete {
            whenOrderFoundAndEditable(refNum) { order ⇒
              OrderUpdater.increaseRemorsePeriod(order)
            }
          }
        } ~
        (post & path("lock") & pathEnd) {
          complete {
            whenOrderFoundAndEditable(refNum) { order ⇒
             OrderUpdater.lock(order, admin)
            }
          }
        } ~
        (post & path("unlock") & pathEnd) {
          complete {
            whenFound(Orders.findByRefNum(refNum).result.headOption.run()) { order ⇒
              OrderUpdater.unlock(order)
            }
          }
        } ~
        (post & path("checkout")) {
          complete {
            whenOrderFoundAndEditable(refNum) { order ⇒ new Checkout(order).checkout }
          }
        } ~
        (post & path("line-items") & entity(as[Seq[UpdateLineItemsPayload]])) { reqItems =>
          complete {
            whenOrderFoundAndEditable(refNum) { order ⇒
              LineItemUpdater.updateQuantities(order, reqItems).flatMap {
                case Xor.Right(_) ⇒ Result.fromFuture(FullOrder.fromOrder(order))
                case Xor.Left(xs) ⇒ Result.failures(xs)
              }
            }
          }
        } ~
        pathPrefix("payment-methods" / "credit-cards") {
          (post & entity(as[payloads.CreditCardPayment]) & pathEnd) { payload ⇒
            complete { OrderPaymentUpdater.addCreditCard(refNum, payload.creditCardId).map(renderNothingOrFailures) }
          } ~
          (patch & entity(as[payloads.CreditCardPayment]) & pathEnd) { payload ⇒
            complete { OrderPaymentUpdater.addCreditCard(refNum, payload.creditCardId).map(renderNothingOrFailures) }
          } ~
          (delete & pathEnd) {
            complete { OrderPaymentUpdater.deleteCreditCard(refNum).map(renderNothingOrFailures) }
          }
        } ~
        pathPrefix("payment-methods" / "gift-cards") {
          (post & entity(as[payloads.GiftCardPayment]) & pathEnd) { payload ⇒
            complete { OrderPaymentUpdater.addGiftCard(refNum, payload).map(renderNothingOrFailures) }
          } ~
          (delete & path(Segment) & pathEnd) { code ⇒
            complete { OrderPaymentUpdater.deleteGiftCard(refNum, code).map(renderNothingOrFailures) }
          }
        } ~
        pathPrefix("payment-methods" / "store-credit") {
          (post & entity(as[payloads.StoreCreditPayment]) & pathEnd) { payload ⇒
            complete { OrderPaymentUpdater.addStoreCredit(refNum, payload).map(renderNothingOrFailures) }
          } ~
          (delete & pathEnd) {
            complete { OrderPaymentUpdater.deleteStoreCredit(refNum).map(renderNothingOrFailures) }
          }
        } ~
        pathPrefix("notes") {
          (get & pathEnd) {
            complete {
              whenOrderFoundAndEditable(refNum) { order ⇒ AdminNotes.forOrder(order) }
            }
          } ~
          (post & entity(as[payloads.CreateNote])) { payload ⇒
            complete {
              whenOrderFoundAndEditable(refNum) { order ⇒
                services.NoteManager.createOrderNote(order, admin, payload)
              }
            }
          } ~
          (patch & path(IntNumber) & entity(as[payloads.UpdateNote])) { (noteId, payload) ⇒
            complete {
              whenOrderFoundAndEditable(refNum) { order ⇒
                services.NoteManager.updateNote(noteId, admin, payload)
              }
            }
          } ~
          (delete & path(IntNumber)) { noteId ⇒
            complete {
              notFoundResponse
            }
          }
        } ~
        pathPrefix("shipping-address") {
          (post & entity(as[payloads.CreateShippingAddress]) & pathEnd) { payload ⇒
            complete {
              whenOrderFoundAndEditable(refNum) { order ⇒
                services.OrderUpdater.createShippingAddress(order, payload)
              }
            }
          } ~
          (patch & entity(as[payloads.UpdateShippingAddress]) & pathEnd) { payload ⇒
            complete {
              whenFound(Orders.findByRefNum(refNum).result.headOption.run()) { order ⇒
                services.OrderUpdater.updateShippingAddress(order, payload)
              }
            }
          } ~
          (delete & pathEnd) {
            complete {
              Orders.findByRefNum(refNum).result.headOption.run().flatMap {
                case Some(order) ⇒
                  services.OrderUpdater.removeShippingAddress(order.id).map { _ ⇒ noContentResponse }
                case None ⇒
                  Future.successful(notFoundResponse)
              }
            }
          }
        }
      }
    }
  }
}

