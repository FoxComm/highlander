package routes.admin

import akka.http.scaladsl.server.Directives._

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import failures.SharedSearchFailures.SharedSearchInvalidQueryFailure
import models.account.User
import models.cord.Cord.cordRefNumRegex
import models.inventory.Sku
import models.payment.giftcard.GiftCard
import models.returns.Return
import models.sharedsearch.SharedSearch
import payloads.NotePayloads._
import payloads.SharedSearchPayloads._
import services.notes._
import services.{SaveForLaterManager, SharedSearchService, ShippingManager}
import services.Authenticator.AuthData
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.Http._

object AdminRoutes {

  def routes(implicit ec: EC, db: DB, auth: AuthData[User], tr: TR, tracer: TEI) = {

    activityContext(auth.model) { implicit ac ⇒
      StoreCreditRoutes.storeCreditRoutes ~
      pathPrefix("shipping-methods" / cordRefNumRegex) { refNum ⇒
        (get & pathEnd) {
          getOrFailures {
            ShippingManager.getShippingMethodsForCart(refNum)
          }
        }
      } ~
      pathPrefix("notes") {
        pathPrefix("order" / cordRefNumRegex) { refNum ⇒
          (get & pathEnd) {
            getOrFailures {
              CordNoteManager.list(refNum)
            }
          } ~
          (post & pathEnd & entity(as[CreateNote])) { payload ⇒
            mutateOrFailures {
              CordNoteManager.create(refNum, auth.model, payload)
            }
          } ~
          (patch & path(IntNumber) & pathEnd & entity(as[UpdateNote])) { (noteId, payload) ⇒
            mutateOrFailures {
              CordNoteManager.update(refNum, noteId, auth.model, payload)
            }
          } ~
          (delete & path(IntNumber)) { noteId ⇒
            deleteOrFailures {
              CordNoteManager.delete(refNum, noteId, auth.model)
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
              GiftCardNoteManager.create(code, auth.model, payload)
            }
          } ~
          path(IntNumber) { noteId ⇒
            (patch & pathEnd & entity(as[UpdateNote])) { payload ⇒
              mutateOrFailures {
                GiftCardNoteManager.update(code, noteId, auth.model, payload)
              }
            } ~
            (delete & pathEnd) {
              deleteOrFailures {
                GiftCardNoteManager.delete(code, noteId, auth.model)
              }
            }
          }
        } ~
        pathPrefix("customer" / IntNumber) { accountId ⇒
          (get & pathEnd) {
            getOrFailures {
              CustomerNoteManager.list(accountId)
            }
          } ~
          (post & pathEnd & entity(as[CreateNote])) { payload ⇒
            mutateOrFailures {
              CustomerNoteManager.create(accountId, auth.model, payload)
            }
          } ~
          path(IntNumber) { noteId ⇒
            (patch & pathEnd & entity(as[UpdateNote])) { payload ⇒
              mutateOrFailures {
                CustomerNoteManager.update(accountId, noteId, auth.model, payload)
              }
            } ~
            (delete & pathEnd) {
              deleteOrFailures {
                CustomerNoteManager.delete(accountId, noteId, auth.model)
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
              ReturnNoteManager.create(refNum, auth.model, payload)
            }
          } ~
          path(IntNumber) { noteId ⇒
            (patch & pathEnd & entity(as[UpdateNote])) { payload ⇒
              mutateOrFailures {
                ReturnNoteManager.update(refNum, noteId, auth.model, payload)
              }
            } ~
            (delete & pathEnd) {
              deleteOrFailures {
                ReturnNoteManager.delete(refNum, noteId, auth.model)
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
              SkuNoteManager.create(code, auth.model, payload)
            }
          } ~
          path(IntNumber) { noteId ⇒
            (patch & pathEnd & entity(as[UpdateNote])) { payload ⇒
              mutateOrFailures {
                SkuNoteManager.update(code, noteId, auth.model, payload)
              }
            } ~
            (delete & pathEnd) {
              deleteOrFailures {
                SkuNoteManager.delete(code, noteId, auth.model)
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
              ProductNoteManager.create(productId, auth.model, payload)
            }
          } ~
          path(IntNumber) { noteId ⇒
            (patch & pathEnd & entity(as[UpdateNote])) { payload ⇒
              mutateOrFailures {
                ProductNoteManager.update(productId, noteId, auth.model, payload)
              }
            } ~
            (delete & pathEnd) {
              deleteOrFailures {
                ProductNoteManager.delete(productId, noteId, auth.model)
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
              PromotionNoteManager.create(promoId, auth.model, payload)
            }
          } ~
          path(IntNumber) { noteId ⇒
            (patch & pathEnd & entity(as[UpdateNote])) { payload ⇒
              mutateOrFailures {
                PromotionNoteManager.update(promoId, noteId, auth.model, payload)
              }
            } ~
            (delete & pathEnd) {
              deleteOrFailures {
                PromotionNoteManager.delete(promoId, noteId, auth.model)
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
              CouponNoteManager.create(couponId, auth.model, payload)
            }
          } ~
          path(IntNumber) { noteId ⇒
            (patch & pathEnd & entity(as[UpdateNote])) { payload ⇒
              mutateOrFailures {
                CouponNoteManager.update(couponId, noteId, auth.model, payload)
              }
            } ~
            (delete & pathEnd) {
              deleteOrFailures {
                CouponNoteManager.delete(couponId, noteId, auth.model)
              }
            }
          }
        } ~
        pathPrefix("store-admins" / IntNumber) { adminId ⇒
          (get & pathEnd) {
            getOrFailures {
              StoreAdminNoteManager.list(adminId)
            }
          } ~
          (post & pathEnd & entity(as[CreateNote])) { payload ⇒
            mutateOrFailures {
              StoreAdminNoteManager.create(adminId, auth.model, payload)
            }
          } ~
          path(IntNumber) { noteId ⇒
            (patch & pathEnd & entity(as[UpdateNote])) { payload ⇒
              mutateOrFailures {
                StoreAdminNoteManager.update(adminId, noteId, auth.model, payload)
              }
            } ~
            (delete & pathEnd) {
              deleteOrFailures {
                StoreAdminNoteManager.delete(adminId, noteId, auth.model)
              }
            }
          }
        }
      } ~
      pathPrefix("save-for-later") {
        determineObjectContext(db, ec) { productContext ⇒
          (get & path(IntNumber) & pathEnd) { accountId ⇒
            getOrFailures {
              SaveForLaterManager.findAll(accountId, productContext.id)
            }
          } ~
          (post & path(IntNumber / Segment) & pathEnd) { (accountId, skuCode) ⇒
            mutateOrFailures {
              SaveForLaterManager.saveForLater(accountId, skuCode, productContext)
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
            SharedSearchService.getAll(auth.model, scope)
          }
        } ~
        (post & pathEnd & entityOr(as[SharedSearchPayload], SharedSearchInvalidQueryFailure)) {
          payload ⇒
            mutateOrFailures {
              SharedSearchService.create(auth.model, payload)
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
              SharedSearchService.update(auth.model, code, payload)
            }
        } ~
        (delete & pathEnd) {
          deleteOrFailures {
            SharedSearchService.delete(auth.model, code)
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
              SharedSearchService.associate(auth.model, code, payload.associates)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { associateId ⇒
            mutateOrFailures {
              SharedSearchService.unassociate(auth.model, code, associateId)
            }
          }
        }
      }
    }
  }
}
