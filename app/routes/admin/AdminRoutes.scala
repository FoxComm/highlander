package routes.admin

import akka.http.scaladsl.server.Directives._

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import failures.SharedSearchFailures.SharedSearchInvalidQueryFailure
import models.StoreAdmin
import models.inventory.Sku
import models.order.Order
import models.order.Order.orderRefNumRegex
import models.payment.giftcard.GiftCard
import models.returns.Return
import models.sharedsearch.SharedSearch
import payloads.NotePayloads._
import payloads.SharedSearchPayloads._
import services.notes._
import services.{SaveForLaterManager, SharedSearchService, ShippingManager}
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.Http._

object AdminRoutes {

  def routes(implicit ec: EC, db: DB, admin: StoreAdmin) = {

    activityContext(admin) { implicit ac ⇒
      StoreCreditRoutes.storeCreditRoutes ~
      pathPrefix("shipping-methods" / orderRefNumRegex) { refNum ⇒
        (get & pathEnd) {
          getOrFailures {
            ShippingManager.getShippingMethodsForOrder(refNum)
          }
        }
      } ~
      pathPrefix("notes") {
        pathPrefix("order" / orderRefNumRegex) { refNum ⇒
          (get & pathEnd) {
            getOrFailures {
              OrderNoteManager.list(refNum)
            }
          } ~
          (post & pathEnd & entity(as[CreateNote])) { payload ⇒
            mutateOrFailures {
              OrderNoteManager.create(refNum, admin, payload)
            }
          } ~
          (patch & path(IntNumber) & pathEnd & entity(as[UpdateNote])) { (noteId, payload) ⇒
            mutateOrFailures {
              OrderNoteManager.update(refNum, noteId, admin, payload)
            }
          } ~
          (delete & path(IntNumber)) { noteId ⇒
            deleteOrFailures {
              OrderNoteManager.delete(refNum, noteId, admin)
            }
          }
        } ~
        pathPrefix("gift-card" / GiftCard.giftCardCodeRegex) { code ⇒
          (get & pathEnd) {
            getOrFailures {
              GiftCardNoteManager.list(code)
            }
          } ~
          (post & pathEnd & entity(as[CreateNote])) { payload ⇒
            mutateOrFailures {
              GiftCardNoteManager.create(code, admin, payload)
            }
          } ~
          path(IntNumber) { noteId ⇒
            (patch & pathEnd & entity(as[UpdateNote])) { payload ⇒
              mutateOrFailures {
                GiftCardNoteManager.update(code, noteId, admin, payload)
              }
            } ~
            (delete & pathEnd) {
              deleteOrFailures {
                GiftCardNoteManager.delete(code, noteId, admin)
              }
            }
          }
        } ~
        pathPrefix("customer" / IntNumber) { customerId ⇒
          (get & pathEnd) {
            getOrFailures {
              CustomerNoteManager.list(customerId)
            }
          } ~
          (post & pathEnd & entity(as[CreateNote])) { payload ⇒
            mutateOrFailures {
              CustomerNoteManager.create(customerId, admin, payload)
            }
          } ~
          path(IntNumber) { noteId ⇒
            (patch & pathEnd & entity(as[UpdateNote])) { payload ⇒
              mutateOrFailures {
                CustomerNoteManager.update(customerId, noteId, admin, payload)
              }
            } ~
            (delete & pathEnd) {
              deleteOrFailures {
                CustomerNoteManager.delete(customerId, noteId, admin)
              }
            }
          }
        } ~
        pathPrefix("return" / Return.returnRefNumRegex) { refNum ⇒
          (get & pathEnd) {
            getOrFailures {
              ReturnNoteManager.list(refNum)
            }
          } ~
          (post & pathEnd & entity(as[CreateNote])) { payload ⇒
            mutateOrFailures {
              ReturnNoteManager.create(refNum, admin, payload)
            }
          } ~
          path(IntNumber) { noteId ⇒
            (patch & pathEnd & entity(as[UpdateNote])) { payload ⇒
              mutateOrFailures {
                ReturnNoteManager.update(refNum, noteId, admin, payload)
              }
            } ~
            (delete & pathEnd) {
              deleteOrFailures {
                ReturnNoteManager.delete(refNum, noteId, admin)
              }
            }
          }
        } ~
        pathPrefix("sku" / Sku.skuCodeRegex) { code ⇒
          (get & pathEnd) {
            getOrFailures {
              SkuNoteManager.list(code)
            }
          } ~
          (post & pathEnd & entity(as[CreateNote])) { payload ⇒
            mutateOrFailures {
              SkuNoteManager.create(code, admin, payload)
            }
          } ~
          path(IntNumber) { noteId ⇒
            (patch & pathEnd & entity(as[UpdateNote])) { payload ⇒
              mutateOrFailures {
                SkuNoteManager.update(code, noteId, admin, payload)
              }
            } ~
            (delete & pathEnd) {
              deleteOrFailures {
                SkuNoteManager.delete(code, noteId, admin)
              }
            }
          }
        } ~
        pathPrefix("product" / IntNumber) { productId ⇒
          (get & pathEnd) {
            getOrFailures {
              ProductNoteManager.list(productId)
            }
          } ~
          (post & pathEnd & entity(as[CreateNote])) { payload ⇒
            mutateOrFailures {
              ProductNoteManager.create(productId, admin, payload)
            }
          } ~
          path(IntNumber) { noteId ⇒
            (patch & pathEnd & entity(as[UpdateNote])) { payload ⇒
              mutateOrFailures {
                ProductNoteManager.update(productId, noteId, admin, payload)
              }
            } ~
            (delete & pathEnd) {
              deleteOrFailures {
                ProductNoteManager.delete(productId, noteId, admin)
              }
            }
          }
        } ~
        pathPrefix("promotion" / IntNumber) { promoId ⇒
          (get & pathEnd) {
            getOrFailures {
              PromotionNoteManager.list(promoId)
            }
          } ~
          (post & pathEnd & entity(as[CreateNote])) { payload ⇒
            mutateOrFailures {
              PromotionNoteManager.create(promoId, admin, payload)
            }
          } ~
          path(IntNumber) { noteId ⇒
            (patch & pathEnd & entity(as[UpdateNote])) { payload ⇒
              mutateOrFailures {
                PromotionNoteManager.update(promoId, noteId, admin, payload)
              }
            } ~
            (delete & pathEnd) {
              deleteOrFailures {
                PromotionNoteManager.delete(promoId, noteId, admin)
              }
            }
          }
        } ~
        pathPrefix("coupon" / IntNumber) { couponId ⇒
          (get & pathEnd) {
            getOrFailures {
              CouponNoteManager.list(couponId)
            }
          } ~
          (post & pathEnd & entity(as[CreateNote])) { payload ⇒
            mutateOrFailures {
              CouponNoteManager.create(couponId, admin, payload)
            }
          } ~
          path(IntNumber) { noteId ⇒
            (patch & pathEnd & entity(as[UpdateNote])) { payload ⇒
              mutateOrFailures {
                CouponNoteManager.update(couponId, noteId, admin, payload)
              }
            } ~
            (delete & pathEnd) {
              deleteOrFailures {
                CouponNoteManager.delete(couponId, noteId, admin)
              }
            }
          }
        }
      } ~
      pathPrefix("save-for-later") {
        determineObjectContext(db, ec) { productContext ⇒
          (get & path(IntNumber) & pathEnd) { customerId ⇒
            getOrFailures {
              SaveForLaterManager.findAll(customerId, productContext.id)
            }
          } ~
          (post & path(IntNumber / Segment) & pathEnd) { (customerId, skuCode) ⇒
            mutateOrFailures {
              SaveForLaterManager.saveForLater(customerId, skuCode, productContext)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { id ⇒
            deleteOrFailures {
              SaveForLaterManager.deleteSaveForLater(id)
            }
          }
        }
      } ~
      pathPrefix("shared-search") {
        (get & pathEnd & parameters('scope.as[String].?)) { scope ⇒
          getOrFailures {
            SharedSearchService.getAll(admin, scope)
          }
        } ~
        (post & pathEnd & entityOr(as[SharedSearchPayload], SharedSearchInvalidQueryFailure)) {
          payload ⇒
            mutateOrFailures {
              SharedSearchService.create(admin, payload)
            }
        }
      } ~
      pathPrefix("shared-search" / SharedSearch.sharedSearchRegex) { code ⇒
        (get & pathEnd) {
          getOrFailures {
            SharedSearchService.get(code)
          }
        } ~
        (patch & pathEnd & entityOr(as[SharedSearchPayload], SharedSearchInvalidQueryFailure)) {
          payload ⇒
            mutateOrFailures {
              SharedSearchService.update(admin, code, payload)
            }
        } ~
        (delete & pathEnd) {
          deleteOrFailures {
            SharedSearchService.delete(admin, code)
          }
        } ~
        pathPrefix("associates") {
          (get & pathEnd) {
            getOrFailures {
              SharedSearchService.getAssociates(code)
            }
          }
        } ~
        pathPrefix("associate") {
          (post & pathEnd & entity(as[SharedSearchAssociationPayload])) { payload ⇒
            mutateOrFailures {
              SharedSearchService.associate(admin, code, payload.associates)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { associateId ⇒
            mutateOrFailures {
              SharedSearchService.unassociate(admin, code, associateId)
            }
          }
        }
      }
    }
  }
}
