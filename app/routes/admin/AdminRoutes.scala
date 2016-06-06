package routes.admin

import akka.http.scaladsl.server.Directives._

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.order.Order
import Order.orderRefNumRegex
import failures.SharedSearchFailures.SharedSearchInvalidQueryFailure
import models.inventory.Sku
import models.payment.giftcard.GiftCard
import models.returns.Return
import models.StoreAdmin
import models.auth.AdminToken
import models.sharedsearch.SharedSearch
import payloads.NotePayloads._
import payloads.SharedSearchPayloads._
import services.{SaveForLaterManager, SharedSearchService, ShippingManager}
import services.notes._
import utils.http.CustomDirectives._
import utils.http.Http._
import utils.aliases._

object AdminRoutes {

  def routes(implicit ec: EC, db: DB, admin: StoreAdmin) = {

    activityContext(admin) { implicit ac ⇒
      (path("admin" / "info") & get) {
        complete(AdminToken.fromAdmin(admin))
      } ~ StoreCreditRoutes.storeCreditRoutes ~
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
              OrderNoteManager.list(refNum)
            }
          } ~
          (post & pathEnd & entity(as[CreateNote])) { payload ⇒
            goodOrFailures {
              OrderNoteManager.create(refNum, admin, payload)
            }
          } ~
          (patch & path(IntNumber) & pathEnd & entity(as[UpdateNote])) { (noteId, payload) ⇒
            goodOrFailures {
              OrderNoteManager.update(refNum, noteId, admin, payload)
            }
          } ~
          (delete & path(IntNumber)) { noteId ⇒
            nothingOrFailures {
              OrderNoteManager.delete(refNum, noteId, admin)
            }
          }
        } ~
        pathPrefix("gift-card" / GiftCard.giftCardCodeRegex) { code ⇒
          (get & pathEnd) {
            goodOrFailures {
              GiftCardNoteManager.list(code)
            }
          } ~
          (post & pathEnd & entity(as[CreateNote])) { payload ⇒
            goodOrFailures {
              GiftCardNoteManager.create(code, admin, payload)
            }
          } ~
          path(IntNumber) { noteId ⇒
            (patch & pathEnd & entity(as[UpdateNote])) { payload ⇒
              goodOrFailures {
                GiftCardNoteManager.update(code, noteId, admin, payload)
              }
            } ~
            (delete & pathEnd) {
              nothingOrFailures {
                GiftCardNoteManager.delete(code, noteId, admin)
              }
            }
          }
        } ~
        pathPrefix("customer" / IntNumber) { customerId ⇒
          (get & pathEnd) {
            goodOrFailures {
              CustomerNoteManager.list(customerId)
            }
          } ~
          (post & pathEnd & entity(as[CreateNote])) { payload ⇒
            goodOrFailures {
              CustomerNoteManager.create(customerId, admin, payload)
            }
          } ~
          path(IntNumber) { noteId ⇒
            (patch & pathEnd & entity(as[UpdateNote])) { payload ⇒
              goodOrFailures {
                CustomerNoteManager.update(customerId, noteId, admin, payload)
              }
            } ~
            (delete & pathEnd) {
              nothingOrFailures {
                CustomerNoteManager.delete(customerId, noteId, admin)
              }
            }
          }
        } ~
        pathPrefix("return" / Return.returnRefNumRegex) { refNum ⇒
          (get & pathEnd) {
            goodOrFailures {
              ReturnNoteManager.list(refNum)
            }
          } ~
          (post & pathEnd & entity(as[CreateNote])) { payload ⇒
            goodOrFailures {
              ReturnNoteManager.create(refNum, admin, payload)
            }
          } ~
          path(IntNumber) { noteId ⇒
            (patch & pathEnd & entity(as[UpdateNote])) { payload ⇒
              goodOrFailures {
                ReturnNoteManager.update(refNum, noteId, admin, payload)
              }
            } ~
            (delete & pathEnd) {
              nothingOrFailures {
                ReturnNoteManager.delete(refNum, noteId, admin)
              }
            }
          }
        } ~
        pathPrefix("sku" / Sku.skuCodeRegex) { code ⇒
          (get & pathEnd) {
            goodOrFailures {
              SkuNoteManager.list(code)
            }
          } ~
          (post & pathEnd & entity(as[CreateNote])) { payload ⇒
            goodOrFailures {
              SkuNoteManager.create(code, admin, payload)
            }
          } ~
          path(IntNumber) { noteId ⇒
            (patch & pathEnd & entity(as[UpdateNote])) { payload ⇒
              goodOrFailures {
                SkuNoteManager.update(code, noteId, admin, payload)
              }
            } ~
            (delete & pathEnd) {
              nothingOrFailures {
                SkuNoteManager.delete(code, noteId, admin)
              }
            }
          }
        } ~
        pathPrefix("product" / IntNumber) { productId ⇒
          (get & pathEnd) {
            goodOrFailures {
              ProductNoteManager.list(productId)
            }
          } ~
          (post & pathEnd & entity(as[CreateNote])) { payload ⇒
            goodOrFailures {
              ProductNoteManager.create(productId, admin, payload)
            }
          } ~
          path(IntNumber) { noteId ⇒
            (patch & pathEnd & entity(as[UpdateNote])) { payload ⇒
              goodOrFailures {
                ProductNoteManager.update(productId, noteId, admin, payload)
              }
            } ~
            (delete & pathEnd) {
              nothingOrFailures {
                ProductNoteManager.delete(productId, noteId, admin)
              }
            }
          }
        } ~
        pathPrefix("promotion" / IntNumber) { promoId ⇒
          (get & pathEnd) {
            goodOrFailures {
              PromotionNoteManager.list(promoId)
            }
          } ~
          (post & pathEnd & entity(as[CreateNote])) { payload ⇒
            goodOrFailures {
              PromotionNoteManager.create(promoId, admin, payload)
            }
          } ~
          path(IntNumber) { noteId ⇒
            (patch & pathEnd & entity(as[UpdateNote])) { payload ⇒
              goodOrFailures {
                PromotionNoteManager.update(promoId, noteId, admin, payload)
              }
            } ~
            (delete & pathEnd) {
              nothingOrFailures {
                PromotionNoteManager.delete(promoId, noteId, admin)
              }
            }
          }
        } ~
        pathPrefix("coupon" / IntNumber) { couponId ⇒
          (get & pathEnd) {
            goodOrFailures {
              CouponNoteManager.list(couponId)
            }
          } ~
          (post & pathEnd & entity(as[CreateNote])) { payload ⇒
            goodOrFailures {
              CouponNoteManager.create(couponId, admin, payload)
            }
          } ~
          path(IntNumber) { noteId ⇒
            (patch & pathEnd & entity(as[UpdateNote])) { payload ⇒
              goodOrFailures {
                CouponNoteManager.update(couponId, noteId, admin, payload)
              }
            } ~
            (delete & pathEnd) {
              nothingOrFailures {
                CouponNoteManager.delete(couponId, noteId, admin)
              }
            }
          }
        }
      } ~
      pathPrefix("save-for-later") {
        determineObjectContext(db, ec) { productContext ⇒
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
        (post & pathEnd & entityOr(as[SharedSearchPayload], SharedSearchInvalidQueryFailure)) {
          payload ⇒
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
        (patch & pathEnd & entityOr(as[SharedSearchPayload], SharedSearchInvalidQueryFailure)) {
          payload ⇒
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
