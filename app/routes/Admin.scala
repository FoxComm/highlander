package routes

import scala.concurrent.{ExecutionContext, Future}
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer

import cats.data.Xor
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import models._
import payloads._
import responses.{AllOrders, AllOrdersWithFailures, AdminNotes, FullOrder, StoreCreditAdjustmentsResponse}
import services._
import slick.driver.PostgresDriver.api._
import utils.Apis
import utils.Slick.implicits._

object Admin {
  def routes(implicit ec: ExecutionContext, db: Database,
    mat: Materializer, storeAdminAuth: AsyncAuthenticator[StoreAdmin], apis: Apis) = {
    import Json4sSupport._
    import utils.Http._

    authenticateBasicAsync(realm = "admin", storeAdminAuth) { admin =>
      pathPrefix("gift-cards") {
        (get & pathEnd) {
          complete {
            GiftCards.sortBy(_.id.desc).result.run().map(render(_))
          }
        } ~
        (get & path(Segment) & pathEnd) { code ⇒
          complete {
            GiftCardService.getByCode(code).map(renderGoodOrFailures)
          }
        } ~
        (get & path(Segment / "transactions") & pathEnd) { code ⇒
          complete {
            GiftCardAdjustmentsService.forGiftCard(code).map(renderGoodOrFailures)
          }
        } ~
        (post & entity(as[payloads.GiftCardCreateByCsr]) & pathEnd) { payload ⇒
          complete {
            GiftCardService.createByAdmin(admin, payload).map(renderGoodOrFailures)
          }
        } ~
        (patch & path(Segment) & entity(as[payloads.GiftCardUpdateStatusByCsr]) & pathEnd) { (code, payload) ⇒
          complete {
            GiftCardService.updateStatusByCsr(code, payload, admin).map(renderGoodOrFailures)
          }
        } ~
        path(Segment / "notes") { code ⇒
          (get & pathEnd) {
            complete {
              whenFound(GiftCards.findByCode(code).one.run()) { giftCard ⇒ AdminNotes.forGiftCard(giftCard) }
            }
          } ~
          (post & entity(as[payloads.CreateNote]) & pathEnd) { payload ⇒
            complete {
              whenFound(GiftCards.findByCode(code).one.run()) { giftCard ⇒
                NoteManager.createGiftCardNote(giftCard, admin, payload)
              }
            }
          }
        } ~
        path(Segment / "notes" / IntNumber) { (code, noteId) ⇒
          (patch & entity(as[payloads.UpdateNote]) & pathEnd) { payload ⇒
            complete {
              whenFound(GiftCards.findByCode(code).one.run()) { _ ⇒
                NoteManager.updateNote(noteId, admin, payload)
              }
            }
          } ~
          (delete & pathEnd) {
            complete {
              NoteManager.deleteNote(noteId, admin).map(renderNothingOrFailures)
            }
          }
        }
      } ~
      pathPrefix("store-credits") {
        (patch & entity(as[payloads.StoreCreditBulkUpdateStatusByCsr]) & pathEnd) { payload ⇒
          complete {
            StoreCreditService.bulkUpdateStatusByCsr(payload).map(renderGoodOrFailures)
          }
        }
      } ~
      pathPrefix("store-credits" / IntNumber) { storeCreditId ⇒
        (get & pathEnd) {
          complete {
            StoreCreditService.getById(storeCreditId).map(renderGoodOrFailures)
          }
        } ~
        (patch & entity(as[payloads.StoreCreditUpdateStatusByCsr]) & pathEnd) { payload ⇒
          complete {
            StoreCreditService.updateStatusByCsr(storeCreditId, payload, admin).map(renderGoodOrFailures)
          }
        } ~
        (get & path("transactions") & pathEnd) {
          complete {
            StoreCreditAdjustmentsService.forStoreCredit(storeCreditId).map(renderGoodOrFailures)
          }
        }
      } ~
      pathPrefix("reasons") {
        (get & pathEnd) {
          complete {
            ReasonService.listAll.map(render(_))
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
                AddressManager.setDefaultShippingAddress(customerId, id).map(renderNothingOrFailures)
              }
          } ~
          (delete & path("default") & pathEnd) {
            complete {
              AddressManager.removeDefaultShippingAddress(customerId).map(renderNothingOrFailures)
            }
          } ~
          (patch & path(IntNumber) & entity(as[CreateAddressPayload]) & pathEnd) { (addressId, payload) =>
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
      } ~
      pathPrefix("orders") {
        (get & pathEnd) {
          complete {
            AllOrders.findAll
          }
        } ~
        (post & entity(as[CreateOrder]) & pathEnd) { payload ⇒
          complete { OrderCreator.createCart(payload).map(renderGoodOrFailures) }
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
            whenFound(Orders.findByRefNum(refNum).one.run()) { order ⇒
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
            LockAwareOrderUpdater.increaseRemorsePeriod(refNum).map(renderGoodOrFailures)
          }
        } ~
        (post & path("lock") & pathEnd) {
          complete {
            LockAwareOrderUpdater.lock(refNum, admin).map(renderGoodOrFailures)
          }
        } ~
        (post & path("unlock") & pathEnd) {
          complete {
            LockAwareOrderUpdater.unlock(refNum).map(renderGoodOrFailures)
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
              LineItemUpdater.updateQuantities(order, reqItems)
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
          (patch & entity(as[payloads.StoreCreditPayment]) & pathEnd) { payload ⇒
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
                NoteManager.createOrderNote(order, admin, payload)
              }
            }
          } ~
          (patch & path(IntNumber) & entity(as[payloads.UpdateNote])) { (noteId, payload) ⇒
            complete {
              whenOrderFoundAndEditable(refNum) { order ⇒
                NoteManager.updateNote(noteId, admin, payload)
              }
            }
          } ~
          (delete & path(IntNumber)) { noteId ⇒
            complete {
              NoteManager.deleteNote(noteId, admin).map(renderNothingOrFailures)
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
              whenFound(Orders.findByRefNum(refNum).one.run()) { order ⇒
                services.OrderUpdater.updateShippingAddress(order, payload)
              }
            }
          } ~
          (delete & pathEnd) {
            complete {
              Orders.findByRefNum(refNum).one.run().flatMap {
                case Some(order) ⇒
                  services.OrderUpdater.removeShippingAddress(order.id).map { _ ⇒ noContentResponse }
                case None ⇒
                  Future.successful(notFoundResponse)
              }
            }
          }
        } ~
        pathPrefix("shipping-methods") {
          (get & pathEnd) {
            complete {
              whenFound(Orders.findByRefNum(refNum).result.headOption.run()) { order ⇒
                services.ShippingManager.getShippingMethodsForOrder(order)
              }
            }
          }
        }
      }
    }
  }
}

