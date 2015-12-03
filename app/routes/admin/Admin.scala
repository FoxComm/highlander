package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.Order.orderRefNumRegex
import models.Reason.reasonTypeRegex
import models.{GiftCard, Notification, Orders, Rma, StoreAdmin}
import services.{NoteManager, ReasonService, SaveForLaterManager, ShippingManager, StoreCreditAdjustmentsService, StoreCreditService}
import slick.driver.PostgresDriver.api._
import utils.Apis
import utils.CustomDirectives._
import utils.Http._

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext

object Admin {

  def routes(implicit ec: ExecutionContext, db: Database,
    mat: Materializer, storeAdminAuth: AsyncAuthenticator[StoreAdmin], apis: Apis) = {

    authenticateBasicAsync(realm = "admin", storeAdminAuth) { admin ⇒
      pathPrefix("store-credits") {
        (get & path("types") & pathEnd) {
          goodOrFailures {
            StoreCreditService.getOriginTypes
          }
        } ~
        (patch & pathEnd & entity(as[payloads.StoreCreditBulkUpdateStatusByCsr])) { payload ⇒
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
        (patch & pathEnd & entity(as[payloads.StoreCreditUpdateStatusByCsr])) { payload ⇒
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
            ReasonService.listReasons
          }
        }
      } ~
      pathPrefix("reasons" / reasonTypeRegex) { reasonType ⇒
        (get & pathEnd & sortAndPage) { implicit sortAndPage ⇒
          goodOrFailures {
            ReasonService.listReasonsByType(reasonType)
          }
        }
      } ~
      pathPrefix("rma-reasons") {
        (get & pathEnd & sortAndPage) { implicit sortAndPage ⇒
          goodOrFailures {
            ReasonService.listRmaReasons
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
          (post & pathEnd & entity(as[payloads.CreateNote])) { payload ⇒
            goodOrFailures {
              NoteManager.createOrderNote(refNum, admin, payload)
            }
          } ~
          (patch & path(IntNumber) & pathEnd & entity(as[payloads.UpdateNote])) { (noteId, payload) ⇒
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
        pathPrefix("gift-card" / GiftCard.giftCardCodeRegex) { code ⇒
          (get & pathEnd) {
            goodOrFailures {
              NoteManager.forGiftCard(code)
            }
          } ~
          (post & pathEnd & entity(as[payloads.CreateNote])) { payload ⇒
            goodOrFailures {
              NoteManager.createGiftCardNote(code, admin, payload)
            }
          } ~
          path(IntNumber) { noteId ⇒
            (patch & pathEnd & entity(as[payloads.UpdateNote])) { payload ⇒
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
          (post & pathEnd & entity(as[payloads.CreateNote])) { payload ⇒
            goodOrFailures {
              NoteManager.createCustomerNote(id, admin, payload)
            }
          } ~
          path(IntNumber) { noteId ⇒
            (patch & pathEnd & entity(as[payloads.UpdateNote])) { payload ⇒
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
        } ~
        pathPrefix("rma" / Rma.rmaRefNumRegex) { refNum ⇒
          (get & pathEnd) {
            goodOrFailures {
              NoteManager.forRma(refNum)
            }
          } ~
          (post & pathEnd & entity(as[payloads.CreateNote])) { payload ⇒
            goodOrFailures {
              NoteManager.createRmaNote(refNum, admin, payload)
            }
          } ~
          path(IntNumber) { noteId ⇒
            (patch & pathEnd & entity(as[payloads.UpdateNote])) { payload ⇒
              goodOrFailures {
                NoteManager.updateRmaNote(refNum, noteId, admin, payload)
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
      } ~
      pathPrefix("save-for-later") {
        (get & path(IntNumber) & pathEnd) { customerId ⇒
          goodOrFailures {
            SaveForLaterManager.findAll(customerId)
          }
        } ~
        (post & path(IntNumber / IntNumber) & pathEnd) { (customerId, skuId) ⇒
          goodOrFailures {
            SaveForLaterManager.saveForLater(customerId, skuId)
          }
        } ~
        (delete & path(IntNumber) & pathEnd) { id ⇒
          nothingOrFailures {
            SaveForLaterManager.deleteSaveForLater(id)
          }
        }
      }
    }
  }
}

