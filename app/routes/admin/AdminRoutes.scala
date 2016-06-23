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
          getGoodOrFailures {
            ShippingManager.getShippingMethodsForOrder(refNum)
          }
        }
      } ~
      pathPrefix("notes") {
        pathPrefix("order" / orderRefNumRegex) { refNum ⇒
          (get & pathEnd) {
            getGoodOrFailures {
              OrderNoteManager.list(refNum)
            }
          } ~
          (post & pathEnd & entity(as[CreateNote])) { payload ⇒
            mutateGoodOrFailures {
              OrderNoteManager.create(refNum, admin, payload)
            }
          } ~
          (patch & path(IntNumber) & pathEnd & entity(as[UpdateNote])) { (noteId, payload) ⇒
            mutateGoodOrFailures {
              OrderNoteManager.update(refNum, noteId, admin, payload)
            }
          } ~
          (delete & path(IntNumber)) { noteId ⇒
            mutateNothingOrFailures {
              OrderNoteManager.delete(refNum, noteId, admin)
            }
          }
        } ~
        pathPrefix("gift-card" / GiftCard.giftCardCodeRegex) { code ⇒
          (get & pathEnd) {
            getGoodOrFailures {
              GiftCardNoteManager.list(code)
            }
          } ~
          (post & pathEnd & entity(as[CreateNote])) { payload ⇒
            mutateGoodOrFailures {
              GiftCardNoteManager.create(code, admin, payload)
            }
          } ~
          path(IntNumber) { noteId ⇒
            (patch & pathEnd & entity(as[UpdateNote])) { payload ⇒
              mutateGoodOrFailures {
                GiftCardNoteManager.update(code, noteId, admin, payload)
              }
            } ~
            (delete & pathEnd) {
              mutateNothingOrFailures {
                GiftCardNoteManager.delete(code, noteId, admin)
              }
            }
          }
        } ~
        pathPrefix("customer" / IntNumber) { customerId ⇒
          (get & pathEnd) {
            getGoodOrFailures {
              CustomerNoteManager.list(customerId)
            }
          } ~
          (post & pathEnd & entity(as[CreateNote])) { payload ⇒
            mutateGoodOrFailures {
              CustomerNoteManager.create(customerId, admin, payload)
            }
          } ~
          path(IntNumber) { noteId ⇒
            (patch & pathEnd & entity(as[UpdateNote])) { payload ⇒
              mutateGoodOrFailures {
                CustomerNoteManager.update(customerId, noteId, admin, payload)
              }
            } ~
            (delete & pathEnd) {
              mutateNothingOrFailures {
                CustomerNoteManager.delete(customerId, noteId, admin)
              }
            }
          }
        } ~
        pathPrefix("return" / Return.returnRefNumRegex) { refNum ⇒
          (get & pathEnd) {
            getGoodOrFailures {
              ReturnNoteManager.list(refNum)
            }
          } ~
          (post & pathEnd & entity(as[CreateNote])) { payload ⇒
            mutateGoodOrFailures {
              ReturnNoteManager.create(refNum, admin, payload)
            }
          } ~
          path(IntNumber) { noteId ⇒
            (patch & pathEnd & entity(as[UpdateNote])) { payload ⇒
              mutateGoodOrFailures {
                ReturnNoteManager.update(refNum, noteId, admin, payload)
              }
            } ~
            (delete & pathEnd) {
              mutateNothingOrFailures {
                ReturnNoteManager.delete(refNum, noteId, admin)
              }
            }
          }
        } ~
        pathPrefix("sku" / Sku.skuCodeRegex) { code ⇒
          (get & pathEnd) {
            getGoodOrFailures {
              SkuNoteManager.list(code)
            }
          } ~
          (post & pathEnd & entity(as[CreateNote])) { payload ⇒
            mutateGoodOrFailures {
              SkuNoteManager.create(code, admin, payload)
            }
          } ~
          path(IntNumber) { noteId ⇒
            (patch & pathEnd & entity(as[UpdateNote])) { payload ⇒
              mutateGoodOrFailures {
                SkuNoteManager.update(code, noteId, admin, payload)
              }
            } ~
            (delete & pathEnd) {
              mutateNothingOrFailures {
                SkuNoteManager.delete(code, noteId, admin)
              }
            }
          }
        } ~
        pathPrefix("product" / IntNumber) { productId ⇒
          (get & pathEnd) {
            getGoodOrFailures {
              ProductNoteManager.list(productId)
            }
          } ~
          (post & pathEnd & entity(as[CreateNote])) { payload ⇒
            mutateGoodOrFailures {
              ProductNoteManager.create(productId, admin, payload)
            }
          } ~
          path(IntNumber) { noteId ⇒
            (patch & pathEnd & entity(as[UpdateNote])) { payload ⇒
              mutateGoodOrFailures {
                ProductNoteManager.update(productId, noteId, admin, payload)
              }
            } ~
            (delete & pathEnd) {
              mutateNothingOrFailures {
                ProductNoteManager.delete(productId, noteId, admin)
              }
            }
          }
        } ~
        pathPrefix("promotion" / IntNumber) { promoId ⇒
          (get & pathEnd) {
            getGoodOrFailures {
              PromotionNoteManager.list(promoId)
            }
          } ~
          (post & pathEnd & entity(as[CreateNote])) { payload ⇒
            mutateGoodOrFailures {
              PromotionNoteManager.create(promoId, admin, payload)
            }
          } ~
          path(IntNumber) { noteId ⇒
            (patch & pathEnd & entity(as[UpdateNote])) { payload ⇒
              mutateGoodOrFailures {
                PromotionNoteManager.update(promoId, noteId, admin, payload)
              }
            } ~
            (delete & pathEnd) {
              mutateNothingOrFailures {
                PromotionNoteManager.delete(promoId, noteId, admin)
              }
            }
          }
        } ~
        pathPrefix("coupon" / IntNumber) { couponId ⇒
          (get & pathEnd) {
            getGoodOrFailures {
              CouponNoteManager.list(couponId)
            }
          } ~
          (post & pathEnd & entity(as[CreateNote])) { payload ⇒
            mutateGoodOrFailures {
              CouponNoteManager.create(couponId, admin, payload)
            }
          } ~
          path(IntNumber) { noteId ⇒
            (patch & pathEnd & entity(as[UpdateNote])) { payload ⇒
              mutateGoodOrFailures {
                CouponNoteManager.update(couponId, noteId, admin, payload)
              }
            } ~
            (delete & pathEnd) {
              mutateNothingOrFailures {
                CouponNoteManager.delete(couponId, noteId, admin)
              }
            }
          }
        }
      } ~
      pathPrefix("save-for-later") {
        determineObjectContext(db, ec) { productContext ⇒
          (get & path(IntNumber) & pathEnd) { customerId ⇒
            getGoodOrFailures {
              SaveForLaterManager.findAll(customerId, productContext.id)
            }
          } ~
          (post & path(IntNumber / Segment) & pathEnd) { (customerId, skuCode) ⇒
            mutateGoodOrFailures {
              SaveForLaterManager.saveForLater(customerId, skuCode, productContext)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { id ⇒
            mutateNothingOrFailures {
              SaveForLaterManager.deleteSaveForLater(id)
            }
          }
        }
      } ~
      pathPrefix("shared-search") {
        (get & pathEnd & parameters('scope.as[String].?)) { scope ⇒
          getGoodOrFailures {
            SharedSearchService.getAll(admin, scope)
          }
        } ~
        (post & pathEnd & entityOr(as[SharedSearchPayload], SharedSearchInvalidQueryFailure)) {
          payload ⇒
            mutateGoodOrFailures {
              SharedSearchService.create(admin, payload)
            }
        }
      } ~
      pathPrefix("shared-search" / SharedSearch.sharedSearchRegex) { code ⇒
        (get & pathEnd) {
          getGoodOrFailures {
            SharedSearchService.get(code)
          }
        } ~
        (patch & pathEnd & entityOr(as[SharedSearchPayload], SharedSearchInvalidQueryFailure)) {
          payload ⇒
            mutateGoodOrFailures {
              SharedSearchService.update(admin, code, payload)
            }
        } ~
        (delete & pathEnd) {
          mutateNothingOrFailures {
            SharedSearchService.delete(admin, code)
          }
        } ~
        pathPrefix("associates") {
          (get & pathEnd) {
            getGoodOrFailures {
              SharedSearchService.getAssociates(code)
            }
          }
        } ~
        pathPrefix("associate") {
          (post & pathEnd & entity(as[SharedSearchAssociationPayload])) { payload ⇒
            mutateGoodOrFailures {
              SharedSearchService.associate(admin, code, payload.associates)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { associateId ⇒
            mutateGoodOrFailures {
              SharedSearchService.unassociate(admin, code, associateId)
            }
          }
        }
      }
    }
  }
}
