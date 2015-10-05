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

object OrderRoutes {

  val orderRefNum = """([a-zA-Z0-9-_]*)""".r

  def routes(implicit ec: ExecutionContext, db: Database,
    mat: Materializer, storeAdminAuth: AsyncAuthenticator[StoreAdmin], apis: Apis) = {

    authenticateBasicAsync(realm = "admin", storeAdminAuth) { admin ⇒

      pathPrefix("orders") {
        (get & pathEnd) {
          complete {
            AllOrders.runFindAll
          }
        } ~
        (post & entity(as[CreateOrder]) & pathEnd) { payload ⇒
          complete { OrderCreator.createCart(payload).map(renderGoodOrFailures) }
        } ~
        (patch & entity(as[BulkUpdateOrdersPayload]) & pathEnd) { payload ⇒
          complete {
            for {
              failures ← OrderUpdater.updateStatuses(payload.referenceNumbers, payload.status)
              orders ← AllOrders.runFindAll
            } yield BulkOrderUpdateResponse(orders, failures)
          }
        } ~
        pathPrefix("assignees") {
          (post & entity(as[BulkAssignment]) & pathEnd) { payload ⇒
            complete {
              BulkOrderUpdater.assign(payload).map(renderGoodOrFailures)
            }
          } ~
          (post & path("delete") & entity(as[BulkAssignment]) & pathEnd) { payload ⇒
            complete {
              BulkOrderUpdater.unassign(payload).map(renderGoodOrFailures)
            }
          }
        }
      } ~
      pathPrefix("orders" / orderRefNum) { refNum ⇒
        (get & pathEnd) {
          complete {
            val finder = Orders.findByRefNum(refNum)
            finder.findOneAndRunIgnoringLock { order ⇒
              DbResult.fromDbio(Slick.fullOrder(finder))
            }.map(renderGoodOrFailures)
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
        (post & path("line-items") & entity(as[Seq[UpdateLineItemsPayload]])) { reqItems ⇒
          complete {
            whenOrderFoundAndEditable(refNum) { order ⇒
              LineItemUpdater.updateQuantities(order, reqItems)
            }
          }
        } ~
        pathPrefix("payment-methods" / "credit-cards") {
          (post & entity(as[payloads.CreditCardPayment]) & pathEnd) { payload ⇒
            complete { OrderPaymentUpdater.addCreditCard(refNum, payload.creditCardId).map(renderGoodOrFailures) }
          } ~
          (patch & entity(as[payloads.CreditCardPayment]) & pathEnd) { payload ⇒
            complete { OrderPaymentUpdater.addCreditCard(refNum, payload.creditCardId).map(renderGoodOrFailures) }
          } ~
          (delete & pathEnd) {
            complete { OrderPaymentUpdater.deleteCreditCard(refNum).map(renderGoodOrFailures) }
          }
        } ~
        pathPrefix("payment-methods" / "gift-cards") {
          (post & entity(as[payloads.GiftCardPayment]) & pathEnd) { payload ⇒
            complete { OrderPaymentUpdater.addGiftCard(refNum, payload).map(renderGoodOrFailures) }
          } ~
            (delete & path(Segment) & pathEnd) { code ⇒
              complete { OrderPaymentUpdater.deleteGiftCard(refNum, code).map(renderGoodOrFailures) }
            }
        } ~
        pathPrefix("payment-methods" / "store-credit") {
          (post & entity(as[payloads.StoreCreditPayment]) & pathEnd) { payload ⇒
            complete { OrderPaymentUpdater.addStoreCredit(refNum, payload).map(renderGoodOrFailures) }
          } ~
          (patch & entity(as[payloads.StoreCreditPayment]) & pathEnd) { payload ⇒
            complete { OrderPaymentUpdater.addStoreCredit(refNum, payload).map(renderGoodOrFailures) }
          } ~
          (delete & pathEnd) {
            complete { OrderPaymentUpdater.deleteStoreCredit(refNum).map(renderGoodOrFailures) }
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
        pathPrefix("assignees") {
          (post & entity(as[Assignment])) { payload ⇒
            complete {
              LockAwareOrderUpdater.assign(refNum, payload.assignees).map(renderGoodOrFailures)
            }
          }
        } ~
        pathPrefix("shipping-address") {
          (post & entity(as[payloads.CreateAddressPayload]) & pathEnd) { payload ⇒
            complete {
              OrderUpdater.createShippingAddressFromPayload(payload, refNum).map(renderGoodOrFailures)
            }
          } ~
          (patch & entity(as[payloads.UpdateAddressPayload]) & pathEnd) { payload ⇒
            complete {
              OrderUpdater.updateShippingAddressFromPayload(payload, refNum).map(renderGoodOrFailures)
            }
          } ~
          (patch & path(IntNumber) & pathEnd) { addressId ⇒
            complete {
              OrderUpdater.createShippingAddressFromAddressId(addressId, refNum).map(renderGoodOrFailures)
            }
          } ~
          (delete & pathEnd) {
            complete {
              OrderUpdater.removeShippingAddress(refNum).map(renderGoodOrFailures)
            }
          }
        }
      }
    }
  }
}
