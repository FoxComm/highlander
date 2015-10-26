package routes.admin

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.Order.orderRefNumRegex
import models._
import services._
import slick.driver.PostgresDriver.api._
import utils.Apis
import utils.Http._
import utils.CustomDirectives._

object Admin {

  def routes(implicit ec: ExecutionContext, db: Database,
    mat: Materializer, storeAdminAuth: AsyncAuthenticator[StoreAdmin], apis: Apis) = {

    authenticateBasicAsync(realm = "admin", storeAdminAuth) { admin ⇒
      pathPrefix("store-credits") {
        (patch & entity(as[payloads.StoreCreditBulkUpdateStatusByCsr]) & pathEnd) { payload ⇒
          goodOrFailures {
            StoreCreditService.bulkUpdateStatusByCsr(payload, admin)
          }
        }
      } ~
      pathPrefix("store-credits" / IntNumber) { storeCreditId ⇒
        (get & pathEnd) {
          goodOrFailures {
            StoreCreditService.getById(storeCreditId)
          }
        } ~
        (patch & entity(as[payloads.StoreCreditUpdateStatusByCsr]) & pathEnd) { payload ⇒
          goodOrFailures {
            StoreCreditService.updateStatusByCsr(storeCreditId, payload, admin)
          }
        } ~
        (get & path("transactions") & pathEnd & sortAndPage) { implicit sortAndPage ⇒
          goodOrFailures {
            StoreCreditAdjustmentsService.forStoreCredit(storeCreditId)
          }
        }
      } ~
      pathPrefix("reasons") {
        (get & pathEnd & sortAndPage) { implicit sortAndPage ⇒
          goodOrFailures {
            ReasonService.listAll
          }
        }
      } ~
      pathPrefix("shipping-methods" / orderRefNumRegex) { refNum ⇒
        (get & pathEnd) {
          goodOrFailures {
            Orders.findByRefNum(refNum).selectOne { order ⇒
              ShippingManager.getShippingMethodsForOrder(order)
            }
          }
        }
      } ~
      pathPrefix("notes") {
        pathPrefix("order" / orderRefNumRegex) { refNum ⇒
          (get & pathEnd) {
            goodOrFailures {
              NoteManager.forOrder(refNum)
            }
          } ~
          (post & entity(as[payloads.CreateNote])) { payload ⇒
            goodOrFailures {
              NoteManager.createOrderNote(refNum, admin, payload)
            }
          } ~
          (patch & path(IntNumber) & entity(as[payloads.UpdateNote])) { (noteId, payload) ⇒
            goodOrFailures {
              NoteManager.updateOrderNote(refNum, noteId, admin, payload)
            }
          } ~
          (delete & path(IntNumber)) { noteId ⇒
            complete {
              NoteManager.deleteNote(noteId, admin).map(renderNothingOrFailures)
            }
          }
        } ~
        pathPrefix("gift-card" / Segment) { code ⇒
          (get & pathEnd) {
            goodOrFailures {
              NoteManager.forGiftCard(code)
            }
          } ~
          (post & entity(as[payloads.CreateNote]) & pathEnd) { payload ⇒
            goodOrFailures {
              NoteManager.createGiftCardNote(code, admin, payload)
            }
          } ~
          path(IntNumber) { noteId ⇒
            (patch & entity(as[payloads.UpdateNote]) & pathEnd) { payload ⇒
              goodOrFailures {
                NoteManager.updateGiftCardNote(code, noteId, admin, payload)
              }
            } ~
            (delete & pathEnd) {
              complete {
                NoteManager.deleteNote(noteId, admin).map(renderNothingOrFailures)
              }
            }
          }
        } ~
        pathPrefix("customer" / IntNumber) { id ⇒
          (get & pathEnd) {
            goodOrFailures {
              NoteManager.forCustomer(id)
            }
          } ~
          (post & entity(as[payloads.CreateNote]) & pathEnd) { payload ⇒
            goodOrFailures {
              NoteManager.createCustomerNote(id, admin, payload)
            }
          } ~
          path(IntNumber) { noteId ⇒
            (patch & entity(as[payloads.UpdateNote]) & pathEnd) { payload ⇒
              goodOrFailures {
                NoteManager.updateCustomerNote(id, noteId, admin, payload)
              }
            } ~
            (delete & pathEnd) {
              nothingOrFailures {
                NoteManager.deleteNote(noteId, admin)
              }
            }
          }
        }
      } ~
      pathPrefix("notifications") {
        (get & pathEnd) {
          good {
            Seq(
              Notification("Delivered", "Shipment Confirmation", "2015-02-15T08:31:45", "jim@bob.com"),
              Notification("Failed", "Order Confirmation", "2015-02-16T09:23:29", "+ (567) 203-8430")
            )
          }
        } ~
        (get & path(IntNumber) & pathEnd) { notificationId ⇒
          good {
            Notification("Failed", "Order Confirmation", "2015-02-16T09:23:29", "+ (567) 203-8430")
          }
        }
      }
    }
  }
}

