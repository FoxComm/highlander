package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._

import models.order.Order
import Order.orderRefNumRegex
import models.payment.giftcard.GiftCard
import models.rma.Rma
import models.StoreAdmin
import models.sharedsearch.SharedSearch
import payloads._
import services.{SaveForLaterManager, SharedSearchService, ShippingManager, StoreCreditAdjustmentsService,
StoreCreditService, SharedSearchInvalidQueryFailure}
import services.notes._
import utils.Apis
import utils.CustomDirectives._
import utils.Http._
import utils.aliases._

object Admin {

  def routes(implicit ec: EC, db: DB, mat: Materializer, admin: StoreAdmin, apis: Apis) = {

    activityContext(admin) { implicit ac ⇒
      pathPrefix("store-credits") {
        (patch & pathEnd & entity(as[StoreCreditBulkUpdateStateByCsr])) { payload ⇒
          goodOrFailures {
            StoreCreditService.bulkUpdateStateByCsr(payload, admin)
          }
        }
      } ~
      pathPrefix("store-credits" / IntNumber) { storeCreditId ⇒
        (get & pathEnd) {
          goodOrFailures {
            StoreCreditService.getById(storeCreditId)
          }
        } ~
        (patch & pathEnd & entity(as[StoreCreditUpdateStateByCsr])) { payload ⇒
          goodOrFailures {
            StoreCreditService.updateStateByCsr(storeCreditId, payload, admin)
          }
        } ~
        (get & path("transactions") & pathEnd & sortAndPage) { implicit sortAndPage ⇒
          goodOrFailures {
            StoreCreditAdjustmentsService.forStoreCredit(storeCreditId)
          }
        }
      } ~
      pathPrefix("shipping-methods" / orderRefNumRegex) { refNum ⇒
        (get & pathEnd) {
          goodOrFailures {
            ShippingManager.getShippingMethodsForOrder(refNum)
          }
        }
      } ~
      pathPrefix("notes") {
        pathPrefix("order" / orderRefNumRegex) { refNum ⇒
          (get & pathEnd) {
            goodOrFailures {
              OrderNoteManager.forOrder(refNum)
            }
          } ~
          (post & pathEnd & entity(as[CreateNote])) { payload ⇒
            goodOrFailures {
              OrderNoteManager.createOrderNote(refNum, admin, payload)
            }
          } ~
          (patch & path(IntNumber) & pathEnd & entity(as[UpdateNote])) { (noteId, payload) ⇒
            goodOrFailures {
              OrderNoteManager.updateOrderNote(refNum, noteId, admin, payload)
            }
          } ~
          (delete & path(IntNumber)) { noteId ⇒
            complete {
              OrderNoteManager.deleteOrderNote(refNum, noteId, admin).map(renderNothingOrFailures)
            }
          }
        } ~
        pathPrefix("gift-card" / GiftCard.giftCardCodeRegex) { code ⇒
          (get & pathEnd) {
            goodOrFailures {
              GiftCardNoteManager.forGiftCard(code)
            }
          } ~
          (post & pathEnd & entity(as[CreateNote])) { payload ⇒
            goodOrFailures {
              GiftCardNoteManager.createGiftCardNote(code, admin, payload)
            }
          } ~
          path(IntNumber) { noteId ⇒
            (patch & pathEnd & entity(as[UpdateNote])) { payload ⇒
              goodOrFailures {
                GiftCardNoteManager.updateGiftCardNote(code, noteId, admin, payload)
              }
            } ~
            (delete & pathEnd) {
              complete {
                GiftCardNoteManager.deleteGiftCardNote(noteId, admin).map(renderNothingOrFailures)
              }
            }
          }
        } ~
        pathPrefix("customer" / IntNumber) { id ⇒
          (get & pathEnd) {
            goodOrFailures {
              CustomerNoteManager.forCustomer(id)
            }
          } ~
          (post & pathEnd & entity(as[CreateNote])) { payload ⇒
            goodOrFailures {
              CustomerNoteManager.createCustomerNote(id, admin, payload)
            }
          } ~
          path(IntNumber) { noteId ⇒
            (patch & pathEnd & entity(as[UpdateNote])) { payload ⇒
              goodOrFailures {
                CustomerNoteManager.updateCustomerNote(id, noteId, admin, payload)
              }
            } ~
            (delete & pathEnd) {
              nothingOrFailures {
                CustomerNoteManager.deleteCustomerNote(noteId, admin)
              }
            }
          }
        } ~
        pathPrefix("rma" / Rma.rmaRefNumRegex) { refNum ⇒
          (get & pathEnd) {
            goodOrFailures {
              RmaNoteManager.forRma(refNum)
            }
          } ~
          (post & pathEnd & entity(as[CreateNote])) { payload ⇒
            goodOrFailures {
              RmaNoteManager.createRmaNote(refNum, admin, payload)
            }
          } ~
          path(IntNumber) { noteId ⇒
            (patch & pathEnd & entity(as[UpdateNote])) { payload ⇒
              goodOrFailures {
                RmaNoteManager.updateRmaNote(refNum, noteId, admin, payload)
              }
            } ~
            (delete & pathEnd) {
              nothingOrFailures {
                RmaNoteManager.deleteRmaNote(noteId, admin)
              }
            }
          }
        }
      } ~
      pathPrefix("save-for-later") {
        determineProductContext(db, ec) { productContext ⇒ 
          (get & path(IntNumber) & pathEnd) { customerId ⇒
            goodOrFailures {
              SaveForLaterManager.findAll(customerId, productContext.id)
            }
          } ~
          (post & path(IntNumber / Segment) & pathEnd) { (customerId, skuCode) ⇒
            goodOrFailures {
              SaveForLaterManager.saveForLater(customerId, skuCode, productContext)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { id ⇒
            nothingOrFailures {
              SaveForLaterManager.deleteSaveForLater(id)
            }
          }
        }
      } ~
      pathPrefix("shared-search") {
        (get & pathEnd & parameters('scope.as[String].?)) { scope ⇒
          goodOrFailures {
            SharedSearchService.getAll(admin, scope)
          }
        } ~
        (post & pathEnd & entityOr(as[SharedSearchPayload], SharedSearchInvalidQueryFailure)) { payload ⇒
          goodOrFailures {
            SharedSearchService.create(admin, payload)
          }
        }
      } ~
      pathPrefix("shared-search" / SharedSearch.sharedSearchRegex) { code ⇒
        (get & pathEnd) {
          goodOrFailures {
            SharedSearchService.get(code)
          }
        } ~
        (patch & pathEnd & entityOr(as[SharedSearchPayload], SharedSearchInvalidQueryFailure)) { payload ⇒
          goodOrFailures {
            SharedSearchService.update(admin, code, payload)
          }
        } ~
        (delete & pathEnd) {
          nothingOrFailures {
            SharedSearchService.delete(admin, code)
          }
        } ~
        pathPrefix("associates") {
          (get & pathEnd) {
            goodOrFailures {
              SharedSearchService.getAssociates(code)
            }
          }
        } ~
        pathPrefix("associate") {
          (post & pathEnd & entity(as[SharedSearchAssociationPayload])) { payload ⇒
            goodOrFailures {
              SharedSearchService.associate(admin, code, payload.associates)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { associateId ⇒
            goodOrFailures {
              SharedSearchService.unassociate(admin, code, associateId)
            }
          }
        }
      }
    }
  }
}
